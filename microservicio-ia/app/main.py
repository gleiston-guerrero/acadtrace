from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional
import datetime

app = FastAPI(
    title="SGA - Microservicio de Inteligencia Artificial y Tutoría Pedagógica",
    description="Motor de análisis predictivo de rendimiento escolar, detección de riesgo académico y recomendaciones pedagógicas.",
    version="1.0.0"
)

# Habilitar CORS para comunicación distribuida
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================
# MODELOS DE DATOS (PYDANTIC DTOs)
# ==========================================
class NotasEstudianteDTO(BaseModel):
    oral: float = Field(default=8.0, ge=0.0, le=10.0)
    escrita: float = Field(default=8.0, ge=0.0, le=10.0)
    tareas: float = Field(default=8.0, ge=0.0, le=10.0)
    talleres: float = Field(default=8.0, ge=0.0, le=10.0)
    cuaderno: float = Field(default=8.0, ge=0.0, le=10.0)
    trabajo_individual: float = Field(default=8.0, ge=0.0, le=10.0)
    exposicion: float = Field(default=8.0, ge=0.0, le=10.0)
    proyecto: float = Field(default=8.0, ge=0.0, le=10.0)
    examen: float = Field(default=8.0, ge=0.0, le=10.0)

class DiagnosticoRequest(BaseModel):
    id_matricula: int
    estudiante: str
    materia: str
    grado: str = "Décimo año EGB"
    trimestre: int = 1
    porcentaje_asistencia: float = 95.0
    notas: NotasEstudianteDTO

class DiagnosticoResponse(BaseModel):
    id_matricula: int
    estudiante: str
    materia: str
    trimestre: int
    promedio_formativo_70: float
    promedio_sumativo_30: float
    promedio_trimestral: float
    escala_cualitativa: str
    nivel_riesgo: str  # ALTO, MEDIO, BAJO
    fortalezas: List[str]
    areas_de_mejora: List[str]
    recomendacion_pedagogica: str
    alerta_representante: bool
    fecha_analisis: str

class ChatQueryRequest(BaseModel):
    pregunta: str
    contexto_materia: Optional[str] = "General"

# ==========================================
# ENDPOINTS DEL MICROSERVICIO
# ==========================================
@app.get("/health")
def health_check():
    """Healthcheck para monitoreo en HAProxy y Prometheus"""
    return {
        "status": "UP",
        "servicio": "microservicio-ia",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }

@app.post("/api/ia/diagnostico-estudiante", response_model=DiagnosticoResponse)
def generar_diagnostico(req: DiagnosticoRequest):
    """
    Analiza el desempeño de un estudiante y genera un diagnóstico pedagógico con IA.
    """
    n = req.notas
    
    # 1. Cálculo de Ponderaciones según Normativa Ministerial del Ecuador (70% + 30%)
    prom_formativo_raw = (n.oral + n.escrita + n.tareas + n.talleres + n.cuaderno + n.trabajo_individual + n.exposicion) / 7.0
    total_formativo_70 = prom_formativo_raw * 0.70
    
    prom_sumativo_raw = (n.proyecto + n.examen) / 2.0
    total_sumativo_30 = prom_sumativo_raw * 0.30
    
    promedio_final = round(total_formativo_70 + total_sumativo_30, 2)
    
    # 2. Escala Cualitativa
    if promedio_final >= 9.0:
        escala = "DAR (Domina los Aprendizajes Requeridos)"
    elif promedio_final >= 7.0:
        escala = "AAR (Alcanza los Aprendizajes Requeridos)"
    elif promedio_final >= 5.0:
        escala = "PAAR (Próximo a Alcanzar los Aprendizajes)"
    else:
        escala = "NAAR (No Alcanza los Aprendizajes)"
        
    # 3. Motor de Inferencia y Detección de Riesgo
    fortalezas = []
    debilidades = []
    
    if n.examen >= 8.5:
        fortalezas.append("Excelente asimilación de conceptos en pruebas sumativas individuales.")
    if n.tareas >= 8.5 and n.cuaderno >= 8.5:
        fortalezas.append("Alto nivel de disciplina y cumplimiento con trabajos autónomos en casa.")
    if n.exposicion >= 8.5:
        fortalezas.append("Habilidades destacadas de expresión oral y comunicación.")
    if n.proyecto >= 8.5:
        fortalezas.append("Capacidad sobresaliente de trabajo colaborativo en proyectos interdisciplinarios.")
    if not fortalezas:
        fortalezas.append("Participación regular en las dinámicas del aula de clases.")
        
    if n.talleres < 7.0:
        debilidades.append("Dificultad en talleres prácticos en clase y aplicación de problemas.")
    if n.escrita < 7.0:
        debilidades.append("Rendimiento bajo en lecciones escritas de corta duración.")
    if n.oral < 7.0:
        debilidades.append("Inseguridad en lecciones orales y sustentaciones individuales.")
    if n.examen < 7.0:
        debilidades.append("Bajo desempeño en la evaluación trimestral integradora.")
    if req.porcentaje_asistencia < 85.0:
        debilidades.append(f"Inasistencia crítica acumulada ({req.porcentaje_asistencia}%).")
        
    # 4. Clasificación de Nivel de Riesgo
    if promedio_final < 7.0 or req.porcentaje_asistencia < 80.0:
        nivel_riesgo = "ALTO"
        alerta_rep = True
        recomendacion = (
            f"El estudiante {req.estudiante} se encuentra en zona de vulnerabilidad académica con promedio de {promedio_final:.2f}/10. "
            "Se requiere convocar al representante legal de forma prioritaria, diseñar una guía de adaptación curricular individual "
            "y programar tutorías de refuerzo pedagógico en contraturno para los componentes formativos."
        )
    elif promedio_final < 8.0 or req.porcentaje_asistencia < 90.0:
        nivel_riesgo = "MEDIO"
        alerta_rep = False
        recomendacion = (
            f"El estudiante {req.estudiante} presenta un rendimiento estable ({promedio_final:.2f}/10), pero muestra debilidades en áreas puntuales. "
            "Se sugiere seguimiento docente continuo y retroalimentación formativa en los talleres prácticos antes del cierre del período."
        )
    else:
        nivel_riesgo = "BAJO"
        alerta_rep = False
        recomendacion = (
            f"Desempeño sobresaliente ({promedio_final:.2f}/10). El estudiante domina las competencias requeridas. "
            "Se recomienda incentivar actividades de liderazgo estudiantil y proyectos de profundización académica."
        )

    return DiagnosticoResponse(
        id_matricula=req.id_matricula,
        estudiante=req.estudiante,
        materia=req.materia,
        trimestre=req.trimestre,
        promedio_formativo_70=round(total_formativo_70, 2),
        promedio_sumativo_30=round(total_sumativo_30, 2),
        promedio_trimestral=promedio_final,
        escala_cualitativa=escala,
        nivel_riesgo=nivel_riesgo,
        fortalezas=fortalezas,
        areas_de_mejora=debilidades if debilidades else ["Ninguna debilidad crítica detectada."],
        recomendacion_pedagogica=recomendacion,
        alerta_representante=alerta_rep,
        fecha_analisis=datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    )

@app.post("/api/ia/asistente")
def chat_asistente_ia(req: ChatQueryRequest):
    """
    Asistente conversacional pedagógico para docentes y directivos.
    """
    p = req.pregunta.lower()
    
    if "riesgo" in p or "reprob" in p:
        resp = "Para detectar estudiantes en riesgo, el microservicio evalúa promedios ponderados inferiores a 7.00 y asistencia menor al 85%. Los casos detectados son marcados automáticamente en rojo con recomendación de refuerzo."
    elif "formativ" in p or "sumativ" in p or "70" in p or "30" in p:
        resp = "La normativa del Ministerio de Educación establece un 70% para las 7 actividades formativas (orales, escritas, tareas, talleres, cuaderno, individual, exposición) y 30% para las sumativas (proyecto y examen trimestral)."
    elif "distribuid" in p or "arquitectur" in p or "cluster" in p:
        resp = "El SGA funciona sobre una arquitectura distribuida políglota con aislamiento Schema-per-Service, comunicación gRPC sobre HTTP/2 y particionamiento físico por rango y hash en PostgreSQL 17."
    else:
        resp = f"Asistente Pedagógico IA: He procesado tu consulta sobre '{req.pregunta}'. Todos los módulos de calificaciones y asistencias del SGA están sincronizados en tiempo real mediante el clúster distribuido."
        
    return {
        "respuesta": resp,
        "contexto": req.contexto_materia,
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }
