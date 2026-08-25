import os
import json
import datetime
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import google.generativeai as genai
from dotenv import load_dotenv

# Cargar variables de entorno desde .env
load_dotenv()

app = FastAPI(
    title="SGA - Microservicio de Inteligencia Artificial (Google Gemini)",
    description="Motor pedagógico con IA Generativa (Gemini 1.5 Flash) para análisis predictivo y tutoría académica.",
    version="2.0.0"
)

# Habilitar CORS para comunicación en red distribuida
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configurar Gemini API
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)

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
    motor_ia: str
    fecha_analisis: str

class ChatQueryRequest(BaseModel):
    pregunta: str
    contexto_materia: Optional[str] = "General"

# ==========================================
# ENDPOINTS
# ==========================================
@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "servicio": "microservicio-ia",
        "motor_llm": "Google Gemini 1.5 Flash" if GEMINI_API_KEY else "Modo Heurístico (Sin API Key)",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }

@app.post("/api/ia/diagnostico-estudiante", response_model=DiagnosticoResponse)
def generar_diagnostico_gemini(req: DiagnosticoRequest):
    """
    Genera un diagnóstico pedagógico integral utilizando Google Gemini 1.5 Flash.
    """
    n = req.notas
    
    # 1. Cálculo Matemático de Base (Normativa Ministerial 70% + 30%)
    prom_form = (n.oral + n.escrita + n.tareas + n.talleres + n.cuaderno + n.trabajo_individual + n.exposicion) / 7.0
    tot_70 = round(prom_form * 0.70, 2)
    
    prom_sum = (n.proyecto + n.examen) / 2.0
    tot_30 = round(prom_sum * 0.30, 2)
    
    prom_final = round(tot_70 + tot_30, 2)

    # 2. Inferencia con Google Gemini si la clave está configurada
    if GEMINI_API_KEY:
        try:
            model = genai.GenerativeModel(
                model_name="gemini-1.5-flash",
                generation_config={"response_mime_type": "application/json"}
            )
            
            prompt = f"""
            Eres un Tutor Pedagógico Senior del Ministerio de Educación del Ecuador.
            Analiza el rendimiento académico del siguiente estudiante:
            - Estudiante: {req.estudiante}
            - Grado/Curso: {req.grado} | Materia: {req.materia} | Trimestre: {req.trimestre}
            - Asistencia Acumulada: {req.porcentaje_asistencia}%
            - Componente Formativo (70%): Oral={n.oral}, Escrita={n.escrita}, Tareas={n.tareas}, Talleres={n.talleres}, Cuaderno={n.cuaderno}, Individual={n.trabajo_individual}, Exposición={n.exposicion}. (Total 70%: {tot_70})
            - Componente Sumativo (30%): Proyecto={n.proyecto}, Examen={n.examen}. (Total 30%: {tot_30})
            - Promedio Trimestral Calculado: {prom_final} / 10.00
            
            Devuelve un objeto JSON ESTRICTO con la siguiente estructura:
            {{
                "escala_cualitativa": "DAR (Domina) o AAR (Alcanza) o PAAR (Próximo) o NAAR (No Alcanza)",
                "nivel_riesgo": "ALTO o MEDIO o BAJO",
                "fortalezas": ["fortaleza 1", "fortaleza 2"],
                "areas_de_mejora": ["área de mejora 1", "área de mejora 2"],
                "recomendacion_pedagogica": "Recomendación técnica detallada para el docente y plan de refuerzo.",
                "alerta_representante": true o false
            }}
            """
            
            response = model.generate_content(prompt)
            data = json.loads(response.text)
            
            return DiagnosticoResponse(
                id_matricula=req.id_matricula,
                estudiante=req.estudiante,
                materia=req.materia,
                trimestre=req.trimestre,
                promedio_formativo_70=tot_70,
                promedio_sumativo_30=tot_30,
                promedio_trimestral=prom_final,
                escala_cualitativa=data.get("escala_cualitativa", "AAR (Alcanza los Aprendizajes)"),
                nivel_riesgo=data.get("nivel_riesgo", "MEDIO"),
                fortalezas=data.get("fortalezas", ["Buen desempeño general."]),
                areas_de_mejora=data.get("areas_de_mejora", ["Reforzar talleres prácticos."]),
                recomendacion_pedagogica=data.get("recomendacion_pedagogica", "Continuar con plan de seguimiento."),
                alerta_representante=data.get("alerta_representante", prom_final < 7.0),
                motor_ia="Google Gemini 1.5 Flash (LLM Generativo)",
                fecha_analisis=datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            )
        except Exception as e:
            print(f"[WARN] Error llamando a Gemini API: {e}. Aplicando Fallback Pedagógico.")

    # 3. Fallback Heurístico Robusto (Si no hay API key o no hay internet)
    escala = "DAR (Domina)" if prom_final >= 9.0 else "AAR (Alcanza)" if prom_final >= 7.0 else "PAAR (Próximo)" if prom_final >= 5.0 else "NAAR (No Alcanza)"
    riesgo = "ALTO" if prom_final < 7.0 or req.porcentaje_asistencia < 85.0 else "MEDIO" if prom_final < 8.2 else "BAJO"
    
    return DiagnosticoResponse(
        id_matricula=req.id_matricula,
        estudiante=req.estudiante,
        materia=req.materia,
        trimestre=req.trimestre,
        promedio_formativo_70=tot_70,
        promedio_sumativo_30=tot_30,
        promedio_trimestral=prom_final,
        escala_cualitativa=escala,
        nivel_riesgo=riesgo,
        fortalezas=[
            "Cumplimiento adecuado en tareas y trabajo autónomo.",
            "Asimilación conceptual en pruebas sumativas individuales."
        ],
        areas_de_mejora=[
            "Reforzar participación en talleres colaborativos y lecciones orales."
        ],
        recomendacion_pedagogica=(
            f"El estudiante {req.estudiante} registra {prom_final:.2f}/10 en {req.materia}. "
            f"{'Se requiere plan de recuperación pedagógica y citación al representante.' if riesgo == 'ALTO' else 'Se sugiere mantener el acompañamiento formativo continuo.'}"
        ),
        alerta_representante=(riesgo == "ALTO"),
        motor_ia="Motor Heurístico Local (Normativa Ministerial 70/30)",
        fecha_analisis=datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    )

@app.post("/api/ia/asistente")
def chat_asistente_gemini(req: ChatQueryRequest):
    """
    Chatbot conversacional pedagógico alimentado por Gemini.
    """
    if GEMINI_API_KEY:
        try:
            model = genai.GenerativeModel("gemini-1.5-flash")
            prompt = f"Eres el Asistente Virtual del SGA para la Escuela Provincias Unidas. Responde de forma concisa y profesional a la siguiente consulta docente: {req.pregunta}"
            res = model.generate_content(prompt)
            return {
                "respuesta": res.text,
                "motor": "Google Gemini 1.5 Flash",
                "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
            }
        except Exception as e:
            pass

    return {
        "respuesta": f"Asistente SGA: Consulta procesada sobre '{req.pregunta}'. El sistema distribuido mantiene sincronizadas las calificaciones y asistencias en los 4 shards de base de datos.",
        "motor": "Motor Asistente Local",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }
