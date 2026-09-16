import csv
import os

def csv_a_markdown(ruta_csv):
    if not os.path.exists(ruta_csv):
        print(f"Error: No se encontró el archivo en {ruta_csv}")
        return

    print("### Matriz de Calidad (Generada Automáticamente)")
    print("*(Nota: Las métricas de disponibilidad de producción no están acreditadas en esta muestra)*\n")

    with open(ruta_csv, 'r', encoding='utf-8') as archivo:
        lector = csv.reader(archivo)
        titulos = next(lector)
        
        # Imprimir cabeceras en formato Markdown
        print("| " + " | ".join(titulos) + " |")
        print("|" + "|".join(["---"] * len(titulos)) + "|")
        
        # Imprimir las 20 filas de la matriz
        for fila in lector:
            print("| " + " | ".join(fila) + " |")

if __name__ == "__main__":
    # Ruta del archivo de mediciones que descubrimos
    ruta = "docs/experimentos/resultados/iso25010.csv"
    csv_a_markdown(ruta)
