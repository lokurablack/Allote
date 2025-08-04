

import google.generativeai as genai
import sys
import os

# Función para cargar variables de entorno desde un archivo .env
def load_dotenv(dotenv_path=".env"):
    if not os.path.exists(dotenv_path):
        print(f"Advertencia: El archivo {dotenv_path} no se encontró.")
        return
    with open(dotenv_path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                key, value = line.split("=", 1)
                os.environ[key.strip()] = value.strip()

# Cargar variables de entorno desde .env
load_dotenv()

# Configura la API key de Gemini desde las variables de entorno
API_KEY = os.getenv("GEMINI_API_KEY")

if not API_KEY:
    print("Error: La variable de entorno GEMINI_API_KEY no fue encontrada.")
    print("Asegúrate de que tu archivo .env contiene 'GEMINI_API_KEY=tu_clave_de_api'")
    sys.exit(1)

genai.configure(api_key=API_KEY)

# Verifica si se proporcionó un texto como argumento
if len(sys.argv) < 2:
    print("Por favor, proporciona el texto que quieres enviar a Gemini.")
    print("Ejemplo: python gemini.py \"¿Cuál es la capital de Francia?\"")
    sys.exit(1)

# Une todos los argumentos para formar el prompt completo
prompt = " ".join(sys.argv[1:])

try:
    model = genai.GenerativeModel('gemini-1.5-flash')
    response = model.generate_content(prompt)
    print(response.text)
except Exception as e:
    print(f"Ha ocurrido un error: {e}")


