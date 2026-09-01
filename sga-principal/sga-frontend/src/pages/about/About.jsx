import Layout from "../../components/Layout";
import { useI18n } from "../../context/I18nContext";
import logo from "../../assets/logo.png";

export default function About() {
  const { t } = useI18n();

  const techStack = [
    { name: "Frontend SPA", tech: "React 19 / Vite / Tailwind CSS / i18n / Dark Mode", desc: "Módulo B: Interfaz responsiva con soporte multi-idioma y temas." },
    { name: "sga-principal", tech: "Java 21 / Spring Boot 3.2 / gRPC Server :9092", desc: "Catálogo maestro, autenticación JWT y control transaccional." },
    { name: "microservicio-secretaria", tech: "Java 21 / Spring Boot / Event Sourcing", desc: "Gestión de admisiones con relojes de Lamport y resúmenes IA." },
    { name: "microservicio-docente", tech: "Python 3.12 / Django REST / Android App", desc: "Registro offline-first con Room SQLite y sincronización de actas." },
    { name: "microservicio-soporte", tech: "Java 21 / Spring Boot / etcd Raft :2379", desc: "Helpdesk con RBAC anti-IDOR, elección de líder y servidor gRPC :9094." },
    { name: "microservicio-ia", tech: "Python FastAPI / Google Gemini 1.5 Flash", desc: "Inferencia inteligente y análisis asistido de rendimiento escolar." },
    { name: "API Gateway & Proxy", tech: "HAProxy 2.9 (Alpine) / Prometheus :8404", desc: "Balanceo Round-Robin, CORS perimetral y exportador nativo de métricas." },
    { name: "Base de Datos", tech: "PostgreSQL 16 (AWS EC2 Multi-Schema)", desc: "Aislamiento lógico por esquema: public, secretaria, docente, soporte." },
    { name: "Observabilidad 360°", tech: "Prometheus 2.51 / cAdvisor 0.49 / Grafana 10.4", desc: "Dashboard de 6 vistas: RPS, P50/P95/P99, 4xx/5xx, Raft, CPU/RAM y E2E móvil." }
  ];

  const teamMembers = [
    { name: "Pedro Leonardo Castro López", role: "sga-principal (Java), DevOps & CI/CD Pipeline en GitHub Actions" },
    { name: "Keyla Lisbeth Buste Caicedo", role: "microservicio-secretaria (Java), Event Sourcing & Integración IA Gemini" },
    { name: "Gregory Steven España Zambrano", role: "microservicio-docente (Django) & App Móvil Android (Kotlin / Room)" },
    { name: "Juliana Romina Emanuel Pino", role: "microservicio-soporte (Java), Documentación & Observabilidad (Grafana/Prometheus)" }
  ];

  return (
    <Layout breadcrumb={[t("nav.home"), t("nav.about")]}>
      <div className="max-w-6xl mx-auto space-y-8">
        
        {/* Cabecera Principal */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-700 p-8 flex flex-col md:flex-row items-center gap-6">
          <img src={logo} alt="Logo SGA" className="w-24 h-24 rounded-2xl object-cover shadow-md border-4 border-slate-100 dark:border-slate-700" />
          <div className="text-center md:text-left space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 text-xs font-semibold">
              <span className="w-2 h-2 rounded-full bg-blue-600 animate-pulse"></span>
              {t("about.version")}
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
              {t("about.title")}
            </h1>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              {t("about.subtitle")} -- <span className="font-semibold">{t("about.client")}</span>
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {t("about.institution")} | {t("about.faculty")} | {t("about.course")}
            </p>
          </div>
        </div>

        {/* Equipo de Desarrollo */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
            <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            {t("about.team_title")}
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {teamMembers.map((member, i) => (
              <div key={i} className="p-4 rounded-xl bg-slate-50 dark:bg-slate-900/50 border border-slate-100 dark:border-slate-700 flex items-start gap-3">
                <div className="w-8 h-8 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center flex-shrink-0 text-sm">
                  {member.name.charAt(0)}
                </div>
                <div>
                  <h3 className="font-bold text-sm text-slate-900 dark:text-white">{member.name}</h3>
                  <p className="text-xs text-slate-600 dark:text-slate-400 mt-0.5">{member.role}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-6 pt-4 border-t border-slate-100 dark:border-slate-700 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 dark:text-slate-400 gap-2">
            <div>
              <span className="font-semibold text-slate-700 dark:text-slate-300">{t("about.teacher_title")}:</span> {t("about.teacher_name")}
            </div>
            <div>{t("about.rights")}</div>
          </div>
        </div>

        {/* Arquitectura y Tecnologías */}
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-700 p-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-2">
            <svg className="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            {t("about.architecture_title")}
          </h2>
          <p className="text-xs text-slate-600 dark:text-slate-400 mb-6">{t("about.arch_desc")}</p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {techStack.map((item, i) => (
              <div key={i} className="p-4 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-900/40 space-y-1.5">
                <span className="text-xs font-black uppercase text-blue-600 dark:text-blue-400 tracking-wider">{item.name}</span>
                <h4 className="font-semibold text-xs text-slate-900 dark:text-white">{item.tech}</h4>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>

      </div>
    </Layout>
  );
}
