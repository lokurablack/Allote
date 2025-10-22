#!/usr/bin/env python3
"""
Scraper del vademécum de SENASA con manejo robusto de navegación y soporte para
actualizaciones incrementales.

Características principales:
    - Instancia el WebDriver sólo cuando se ejecuta el script (no al importar).
    - Navega página a página hasta que no existen más resultados.
    - Evita volver a procesar productos ya conocidos (por número de registro).
    - Usa esperas explícitas en lugar de sleeps arbitrarios siempre que es posible.
    - Permite exportar únicamente los productos nuevos detectados.
"""

from __future__ import annotations

import argparse
import csv
import re
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple

from selenium import webdriver
from selenium.common.exceptions import (
    NoSuchElementException,
    TimeoutException,
)
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from webdriver_manager.chrome import ChromeDriverManager


BASE_URL = "https://aps2.senasa.gov.ar/vademecum/app/publico/formulados"
DEFAULT_OUTPUT = "productos_senasa_nuevos.csv"
DEFAULT_EXISTING = "productos_senasa_seguro.csv"

LOG_SYMBOLS: Dict[str, str] = {
    "INFO": "[i]",
    "SUCCESS": "[+]",
    "WARNING": "[!]",
    "ERROR": "[x]",
    "PROGRESS": "[>]",
    "SKIP": "[-]",
    "DEBUG": "[d]",
}


def log_progress(message: str, level: str = "INFO") -> None:
    timestamp = time.strftime("%H:%M:%S")
    print(f"[{timestamp}] {LOG_SYMBOLS.get(level, '[ ]')} {message}")


def xpath_literal(text: str) -> str:
    """Escapa texto para uso en expresiones XPath."""
    if "'" not in text:
        return f"'{text}'"
    if '"' not in text:
        return f'"{text}"'
    parts = text.split("'")
    concat_parts = ", \"'\", ".join(f"'{part}'" for part in parts)
    return f"concat({concat_parts})"


def normalize_registro(value: str) -> str:
    return (value or "").strip()


def load_known_registros(files: Sequence[Path]) -> Set[str]:
    """Lee archivos CSV existentes y devuelve el conjunto de números de registro."""
    registros: Set[str] = set()
    candidate_fields = {"numero_registro", "numeroRegistro", "numero", "registro"}

    for file_path in files:
        if not file_path or not file_path.exists():
            continue

        try:
            with file_path.open("r", encoding="utf-8-sig", newline="") as csv_file:
                reader = csv.DictReader(csv_file, delimiter=";")
                for row in reader:
                    if not row:
                        continue
                    for field in candidate_fields:
                        if field in row and row[field]:
                            registros.add(normalize_registro(row[field]))
                            break
        except Exception as exc:
            log_progress(f"No se pudo leer {file_path}: {exc}", "WARNING")

    return registros


def write_csv(path: Path, records: Iterable["ProductRecord"]) -> None:
    """Escribe el listado de productos nuevos a disco."""
    path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = [
        "numero_registro",
        "marca",
        "activos",
        "banda_tox",
        "aptitudes",
        "presentacion",
    ]
    with path.open("w", encoding="utf-8-sig", newline="") as csv_file:
        writer = csv.DictWriter(
            csv_file,
            fieldnames=fieldnames,
            delimiter=";",
            quoting=csv.QUOTE_ALL,
        )
        writer.writeheader()
        for record in records:
            writer.writerow(record.as_dict())


@dataclass
class ProductSummary:
    numero_registro: str
    marca: str
    activos: str
    banda_tox: str


@dataclass
class ProductRecord(ProductSummary):
    aptitudes: str = ""
    presentacion: str = ""

    def as_dict(self) -> Dict[str, str]:
        return {
            "numero_registro": self.numero_registro,
            "marca": self.marca,
            "activos": self.activos,
            "banda_tox": self.banda_tox,
            "aptitudes": self.aptitudes,
            "presentacion": self.presentacion,
        }


class SenasaScraper:
    def __init__(
        self,
        headless: bool = False,
        wait_timeout: int = 20,
        click_delay: float = 0.4,
        retry_attempts: int = 2,
        page_load_timeout: int = 120,
        command_timeout: int = 180,
    ) -> None:
        self.headless = headless
        self.wait_timeout = wait_timeout
        self.click_delay = click_delay
        self.retry_attempts = retry_attempts
        self.page_load_timeout = page_load_timeout
        self.command_timeout = command_timeout
        self.driver: Optional[webdriver.Chrome] = None
        self.wait: Optional[WebDriverWait] = None
        self.stats: Dict[str, int] = {
            "success": 0,
            "partial": 0,
            "failed": 0,
            "skipped": 0,
        }

    # --------------------------------------------------------------------- #
    # Context manager helpers
    # --------------------------------------------------------------------- #
    def __enter__(self) -> "SenasaScraper":
        self.driver = self._build_driver()
        if self.command_timeout:
            try:
                self.driver.command_executor.set_timeout(self.command_timeout)
            except Exception:
                pass
        if self.page_load_timeout:
            try:
                self.driver.set_page_load_timeout(self.page_load_timeout)
            except Exception:
                pass
        self.wait = WebDriverWait(self.driver, self.wait_timeout)
        return self

    def __exit__(self, exc_type, exc, exc_tb) -> None:
        if self.driver:
            self.driver.quit()

    def _build_driver(self) -> webdriver.Chrome:
        options = webdriver.ChromeOptions()
        if self.headless:
            options.add_argument("--headless=new")
        options.add_argument("--disable-blink-features=AutomationControlled")
        options.add_argument("--disable-extensions")
        options.add_argument("--disable-gpu")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--start-maximized")
        options.add_experimental_option("excludeSwitches", ["enable-automation"])
        options.add_experimental_option("useAutomationExtension", False)
        options.add_argument("--remote-allow-origins=*")

        service = Service(ChromeDriverManager().install())
        return webdriver.Chrome(service=service, options=options)

    # --------------------------------------------------------------------- #
    # Navegación principal
    # --------------------------------------------------------------------- #
    def navigate_to_listing(self) -> None:
        assert self.driver and self.wait
        for attempt in range(2):
            try:
                self.driver.get(BASE_URL)
                break
            except Exception as exc:
                if attempt == 1:
                    raise
                log_progress(f"Reintentando carga inicial por error: {exc}", "WARNING")
                time.sleep(3)
        self._wait_for_table()

    def _wait_for_table(self) -> None:
        assert self.wait
        self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "table tbody tr")))

    def scrape(
        self,
        known_registros: Set[str],
        max_pages: Optional[int] = None,
    ) -> List[ProductRecord]:
        assert self.driver

        self.navigate_to_listing()
        new_products: List[ProductRecord] = []
        processed_this_run: Set[str] = set()
        page_number = 1

        while True:
            try:
                summaries = self._collect_page_products()
            except TimeoutException:
                log_progress("No se pudo cargar la tabla de la página actual", "WARNING")
                break

            if not summaries:
                log_progress("La página no contenía filas, deteniendo scraping", "WARNING")
                break

            log_progress(
                f"Procesando página {page_number} con {len(summaries)} filas",
                "PROGRESS",
            )

            for summary in summaries:
                registro = normalize_registro(summary.numero_registro)
                if not registro:
                    continue

                if registro in known_registros:
                    self.stats["skipped"] += 1
                    continue

                if registro in processed_this_run:
                    self.stats["skipped"] += 1
                    continue

                record = self._process_product(summary)
                new_products.append(record)
                known_registros.add(registro)
                processed_this_run.add(registro)

            if max_pages and page_number >= max_pages:
                log_progress("Se alcanzó el límite de páginas solicitado", "INFO")
                break

            if not self._go_to_next_page():
                break

            page_number += 1

        return new_products

    def _collect_page_products(self) -> List[ProductSummary]:
        assert self.driver and self.wait
        self._wait_for_table()
        rows = self.driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
        summaries: List[ProductSummary] = []

        for row in rows:
            cells = row.find_elements(By.TAG_NAME, "td")
            if len(cells) < 5:
                continue
            numero_registro = normalize_registro(cells[0].text)
            marca = cells[1].text.strip()
            activos = cells[3].text.strip()
            banda_tox = cells[4].text.strip()

            if numero_registro:
                summaries.append(
                    ProductSummary(
                        numero_registro=numero_registro,
                        marca=marca,
                        activos=activos,
                        banda_tox=banda_tox,
                    )
                )

        return summaries

    # --------------------------------------------------------------------- #
    # Procesamiento individual de productos
    # --------------------------------------------------------------------- #
    def _process_product(self, summary: ProductSummary) -> ProductRecord:
        last_error: Optional[Exception] = None

        for attempt in range(1, self.retry_attempts + 1):
            try:
                record, extracted = self._attempt_process(summary, allow_retry=attempt < self.retry_attempts)
                if extracted == "complete":
                    self.stats["success"] += 1
                elif extracted == "partial":
                    self.stats["partial"] += 1
                else:
                    self.stats["failed"] += 1
                return record
            except Exception as exc:
                last_error = exc
                log_progress(
                    f"Error procesando {summary.numero_registro} (intento {attempt}/{self.retry_attempts}): {exc}",
                    "WARNING",
                )
                time.sleep(1.5)

        self.stats["failed"] += 1
        log_progress(
            f"No se pudo extraer información para {summary.numero_registro}: {last_error}",
            "ERROR",
        )
        return ProductRecord(
            numero_registro=summary.numero_registro,
            marca=summary.marca,
            activos=summary.activos,
            banda_tox=summary.banda_tox,
            aptitudes="",
            presentacion="",
        )

    def _attempt_process(
        self,
        summary: ProductSummary,
        allow_retry: bool,
    ) -> Tuple[ProductRecord, str]:
        assert self.driver

        row = self._find_row_by_registro(summary.numero_registro)
        if row is None:
            raise RuntimeError("No se encontró la fila correspondiente en la tabla")

        detail_button = self._find_detail_button(row)
        if detail_button is None:
            raise RuntimeError("No se encontró el botón de detalle en la fila")

        handles_before = self.driver.window_handles[:]
        self.driver.execute_script("arguments[0].click();", detail_button)
        time.sleep(self.click_delay)

        opened_new_tab = self._switch_to_new_tab(handles_before)

        try:
            self._wait_for_detail_page()
            aptitudes, presentacion = self._extract_detail_info(summary.numero_registro)
        finally:
            self._return_to_listing(handles_before, opened_new_tab)

        aptitudes = aptitudes.strip()
        presentacion = presentacion.strip()

        if allow_retry and not (aptitudes or presentacion):
            raise RuntimeError("La página de detalle no devolvió datos")

        extraction_state = "complete"
        if aptitudes and not presentacion or presentacion and not aptitudes:
            extraction_state = "partial"
        if not aptitudes and not presentacion:
            extraction_state = "failed"

        record = ProductRecord(
            numero_registro=summary.numero_registro,
            marca=summary.marca,
            activos=summary.activos,
            banda_tox=summary.banda_tox,
            aptitudes=aptitudes,
            presentacion=presentacion,
        )
        return record, extraction_state

    def _find_row_by_registro(self, numero_registro: str):
        assert self.wait
        registro_literal = xpath_literal(numero_registro)
        xpath = f"//table//tr[td[1][normalize-space()={registro_literal}]]"
        try:
            return self.wait.until(EC.presence_of_element_located((By.XPATH, xpath)))
        except TimeoutException:
            return None

    def _find_detail_button(self, row):
        detail_selectors = [
            "td:last-child a",
            "td:last-child button",
            "td:last-child [onclick]",
            "td:last-child i.fa-search",
            "td:last-child [class*='search']",
            "td:last-child [class*='detail']",
            "td:last-child [title*='detalle']",
        ]
        for selector in detail_selectors:
            try:
                elements = row.find_elements(By.CSS_SELECTOR, selector)
                for element in elements:
                    if element.is_displayed() and element.is_enabled():
                        return element
            except Exception:
                continue
        return None

    def _switch_to_new_tab(self, handles_before: Sequence[str]) -> bool:
        assert self.driver
        handles_after = self.driver.window_handles
        if len(handles_after) > len(handles_before):
            new_handle = next(handle for handle in handles_after if handle not in handles_before)
            self.driver.switch_to.window(new_handle)
            return True
        return False

    def _wait_for_detail_page(self) -> None:
        assert self.wait
        detail_indicators = [
            (By.XPATH, "//*[contains(translate(., 'DATOS', 'datos'), 'datos del producto')]"),
            (By.XPATH, "//*[contains(@class, 'panel-heading') and contains(translate(., 'DATOS', 'datos'), 'datos del producto')]"),
        ]
        for by, locator in detail_indicators:
            try:
                self.wait.until(EC.presence_of_element_located((by, locator)))
                return
            except TimeoutException:
                continue
        raise TimeoutException("No se detectó la vista de detalle del producto")

    def _return_to_listing(self, handles_before: Sequence[str], closed_new_tab: bool) -> None:
        assert self.driver

        if closed_new_tab:
            self.driver.close()
            self.driver.switch_to.window(handles_before[0])
        else:
            if len(self.driver.window_handles) > 1:
                for handle in self.driver.window_handles:
                    if handle in handles_before:
                        continue
                    self.driver.switch_to.window(handle)
                    self.driver.close()
                self.driver.switch_to.window(handles_before[0])
            else:
                self.driver.back()
                time.sleep(self.click_delay)

        self._wait_for_table()

    # --------------------------------------------------------------------- #
    # Extracción de detalle
    # --------------------------------------------------------------------- #
    def _extract_detail_info(self, numero_registro: str) -> Tuple[str, str]:
        if not self._expand_detail_section(numero_registro):
            log_progress(f"No se pudo expandir 'Datos del producto' para {numero_registro}", "WARNING")
            return "", ""

        html = (self.driver.page_source if self.driver else "") or ""
        aptitudes = self._search_patterns(
            html,
            [
                r"Aptitudes:\s*([^<\n\r]+)",
                r"aptitudes?:\s*([^<\n\r]+)",
                r"<[^>]*>Aptitudes[^>]*>\s*([^<]+)",
                r"(?:IN|HE|FU|AC)\s*-\s*(?:Insecticida|Herbicida|Fungicida|Acaricida)",
            ],
        )
        presentacion = self._search_patterns(
            html,
            [
                r"Presentaci[oó]n:\s*([^<\n\r]+)",
                r"<[^>]*>Presentaci[oó]n[^>]*>\s*([^<]+)",
                r"(Suspensi[oó]n concentrada|Polvo mojable|Concentrado emulsionable|SC|WP|EC|SL|SE)",
            ],
        )

        if not aptitudes or not presentacion:
            aptitudes_dom, presentacion_dom = self._search_keywords_in_dom()
            aptitudes = aptitudes or aptitudes_dom
            presentacion = presentacion or presentacion_dom

        return aptitudes, presentacion

    def _expand_detail_section(self, numero_registro: str) -> bool:
        assert self.wait and self.driver
        selectors = [
            "//h4[contains(translate(., 'DATOS', 'datos'), 'datos del producto')]",
            "//div[contains(translate(., 'DATOS', 'datos'), 'datos del producto')]",
            "//button[contains(translate(., 'DATOS', 'datos'), 'datos del producto')]",
            "//a[contains(translate(., 'DATOS', 'datos'), 'datos del producto')]",
            "//*[normalize-space()='Datos del producto']",
        ]

        for selector in selectors:
            try:
                element = self.wait.until(EC.element_to_be_clickable((By.XPATH, selector)))
            except TimeoutException:
                continue

            try:
                self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
            except Exception:
                pass

            self.driver.execute_script("arguments[0].click();", element)
            time.sleep(self.click_delay)

            if self._section_has_loaded():
                return True

        log_progress(f"No se pudo abrir la sección de datos para {numero_registro}", "WARNING")
        return False

    def _section_has_loaded(self) -> bool:
        assert self.driver
        keywords = ["aptitud", "presentaci", "insecticida", "herbicida", "fungicida"]
        page_content = self.driver.page_source.lower()
        return any(keyword in page_content for keyword in keywords)

    def _search_patterns(self, html: str, patterns: Sequence[str]) -> str:
        for pattern in patterns:
            try:
                matches = re.findall(pattern, html, re.IGNORECASE | re.MULTILINE)
            except re.error:
                continue
            for match in matches:
                cleaned = re.sub(r"<[^>]+>", "", str(match)).strip()
                if cleaned and not cleaned.isdigit():
                    return cleaned
        return ""

    def _search_keywords_in_dom(self) -> Tuple[str, str]:
        assert self.driver
        try:
            elements = self.driver.find_elements(
                By.XPATH,
                "//*[contains(translate(text(),'APTUCIONES','aptuciones'),'aptitud') "
                "or contains(translate(text(),'PRESENTACION','presentacion'),'presentac')]",
            )
        except Exception:
            return "", ""

        aptitudes = ""
        presentacion = ""
        for element in elements:
            text = element.text.strip()
            if not text:
                continue
            lower = text.lower()
            if "aptitud" in lower and not aptitudes:
                aptitudes = text
            if "presentac" in lower and not presentacion:
                presentacion = text
            if aptitudes and presentacion:
                break
        return aptitudes, presentacion

    # --------------------------------------------------------------------- #
    # Paginación
    # --------------------------------------------------------------------- #
    def _get_current_page_number(self) -> Optional[int]:
        assert self.driver
        selectors = [
            ("css selector", "ul.pagination li.active"),
            ("css selector", ".pagination .active"),
            ("css selector", ".pagination .current"),
        ]
        for by_str, selector in selectors:
            elements = self.driver.find_elements(getattr(By, by_str.upper().replace(" ", "_")), selector)
            for element in elements:
                text = element.text.strip()
                if text.isdigit():
                    return int(text)
        return None

    def _go_to_next_page(self) -> bool:
        current_page = self._get_current_page_number()
        target_page = current_page + 1 if current_page else None

        if target_page and self._go_to_page(target_page):
            log_progress(f"Cargada página {target_page}", "PROGRESS")
            return True

        next_selectors = [
            (By.XPATH, "//a[contains(translate(., 'SIGUIENTE', 'siguiente'), 'siguiente') and not(contains(@class,'disabled'))]"),
            (By.CSS_SELECTOR, "ul.pagination li.next:not(.disabled) a"),
            (By.CSS_SELECTOR, ".pagination .page-link[rel='next']"),
        ]

        for by, selector in next_selectors:
            elements = self.driver.find_elements(by, selector)
            for element in elements:
                classes = (element.get_attribute("class") or "").lower()
                aria_disabled = (element.get_attribute("aria-disabled") or "").lower()
                if "disabled" in classes or aria_disabled == "true":
                    continue
                try:
                    element.click()
                except Exception:
                    self.driver.execute_script("arguments[0].click();", element)
                time.sleep(self.click_delay)
                self._wait_for_table()
                return True

        log_progress("No se detectaron más páginas para navegar", "INFO")
        return False

    def _go_to_page(self, target_page: int) -> bool:
        assert self.driver
        selectors = [
            f"//a[normalize-space()='{target_page}']",
            f"//a[contains(@href, 'page={target_page}')]",
            f".pagination a[href*='page={target_page}']",
        ]

        for selector in selectors:
            try:
                if selector.startswith("//"):
                    elements = self.driver.find_elements(By.XPATH, selector)
                else:
                    elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
            except Exception:
                continue

            for element in elements:
                if not element.is_displayed() or not element.is_enabled():
                    continue
                try:
                    element.click()
                except Exception:
                    self.driver.execute_script("arguments[0].click();", element)
                time.sleep(self.click_delay)
                self._wait_for_table()
                return True

        return False


# ------------------------------------------------------------------------- #
# CLI
# ------------------------------------------------------------------------- #
def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scraper del vademécum SENASA con soporte incremental.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(DEFAULT_OUTPUT),
        help="Archivo CSV donde se almacenarán los productos nuevos (por defecto: %(default)s).",
    )
    parser.add_argument(
        "--existing-csv",
        type=Path,
        default=Path(DEFAULT_EXISTING),
        help="CSV con datos previamente importados para evitar duplicados.",
    )
    parser.add_argument(
        "--headless",
        action="store_true",
        help="Ejecuta el navegador en modo headless.",
    )
    parser.add_argument(
        "--max-pages",
        type=int,
        default=None,
        help="Límite opcional de páginas a procesar.",
    )
    parser.add_argument(
        "--retry-attempts",
        type=int,
        default=2,
        help="Cantidad de reintentos para cada producto.",
    )
    parser.add_argument(
        "--click-delay",
        type=float,
        default=0.4,
        help="Retraso (segundos) tras cada click para estabilizar la UI.",
    )
    parser.add_argument(
        "--wait-timeout",
        type=int,
        default=20,
        help="Timeout (segundos) para esperas explícitas.",
    )
    parser.add_argument(
        "--page-load-timeout",
        type=int,
        default=120,
        help="Timeout (segundos) para la carga de páginas.",
    )
    parser.add_argument(
        "--command-timeout",
        type=int,
        default=180,
        help="Timeout (segundos) para comandos enviados al navegador.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_arguments()

    existing_files = [args.existing_csv]
    if args.output != args.existing_csv and args.output.exists():
        existing_files.append(args.output)

    known_registros = load_known_registros(existing_files)
    log_progress(f"Productos ya registrados: {len(known_registros)}", "INFO")

    with SenasaScraper(
        headless=args.headless,
        wait_timeout=args.wait_timeout,
        click_delay=args.click_delay,
        retry_attempts=args.retry_attempts,
        page_load_timeout=args.page_load_timeout,
        command_timeout=args.command_timeout,
    ) as scraper:
        start_time = time.time()
        new_products = scraper.scrape(known_registros, max_pages=args.max_pages)
        elapsed = time.time() - start_time

        log_progress("Resumen de scraping:", "INFO")
        for key, value in scraper.stats.items():
            log_progress(f"  {key}: {value}", "INFO")
        log_progress(f"Tiempo total: {elapsed/60:.1f} minutos", "INFO")

    if not new_products:
        log_progress("No se detectaron productos nuevos.", "INFO")
        return

    write_csv(args.output, new_products)
    log_progress(f"Productos nuevos guardados en {args.output}", "SUCCESS")


if __name__ == "__main__":
    main()
