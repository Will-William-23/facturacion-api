import os
import sys

# --- ENCABEZADO UNIVERSAL PARA REPARACIONES ---
# Este bloque busca automáticamente la carpeta del proyecto
# sin importar desde qué terminal lo ejecutes.

def obtener_ruta_base():
    # 1. Donde está este archivo .py
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 2. Buscamos la carpeta 'src'
    # Opción A: Está en la misma carpeta (Ideal)
    ruta_src = os.path.join(script_dir, "src", "main", "java", "com", "tarea", "facturacion_api")
    
    if os.path.exists(ruta_src):
        return ruta_src
        
    # Opción B: Está una carpeta más adentro (Por si ejecutas desde la carpeta madre)
    ruta_src_interna = os.path.join(script_dir, "facturacion-api", "src", "main", "java", "com", "tarea", "facturacion_api")
    if os.path.exists(ruta_src_interna):
        return ruta_src_interna
        
    print(f"❌ ERROR CRÍTICO: No encuentro la carpeta 'src' desde: {script_dir}")
    print("Asegúrate de guardar este archivo junto al pom.xml")
    sys.exit()

# Variable global para usar en tus scripts
BASE_DIR = obtener_ruta_base()
print(f"✅ Ruta base detectada: {BASE_DIR}")
print("-" * 50)

# ==========================================
# PEGA AQUÍ ABAJO EL CÓDIGO NUEVO QUE TE DÉ
# ==========================================

