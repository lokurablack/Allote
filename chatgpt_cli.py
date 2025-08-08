import openai
import os
import json
import glob

openai.api_key = os.getenv("OPENAI_API_KEY")
HISTORIAL_FILE = "historial_chat.json"

if os.path.exists(HISTORIAL_FILE):
    with open(HISTORIAL_FILE, "r", encoding="utf-8") as f:
        historial = json.load(f)
else:
    historial = []

print("💬 ChatGPT CLI - Comando especial: analizar <ruta> para enviar un archivo.\n")

while True:
    prompt = input("Tú: ")

    if prompt.lower() in ["salir", "exit", "quit"]:
        with open(HISTORIAL_FILE, "w", encoding="utf-8") as f:
            json.dump(historial, f, ensure_ascii=False, indent=2)
        break

    # Si el usuario escribe "analizar archivo.ext"
    if prompt.lower().startswith("analizar "):
        ruta = prompt.split(" ", 1)[1]
        try:
            with open(ruta, "r", encoding="utf-8", errors="ignore") as f:
                contenido = f.read()
            prompt = f"Analiza el siguiente archivo de mi app:\n\n{contenido}"
        except FileNotFoundError:
            print(f"⚠️ No se encontró el archivo {ruta}")
            continue

    historial.append({"role": "user", "content": prompt})

    try:
        respuesta = openai.ChatCompletion.create(
            model="gpt-5",  # o gpt-5 si tenés acceso
            messages=historial
        )
        mensaje = respuesta.choices[0].message["content"]
        print("ChatGPT:", mensaje, "\n")
        historial.append({"role": "assistant", "content": mensaje})

    except Exception as e:
        print("⚠️ Error:", e)
