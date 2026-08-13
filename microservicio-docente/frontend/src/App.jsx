import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import PanelPrincipal from "./pages/panel-principal/PanelPrincipal";
import Actividades from "./pages/actividades/Actividades";
import Asistencia from "./pages/asistencia/Asistencia";
import Calificaciones from "./pages/calificaciones/Calificaciones";
import Seguimiento from "./pages/seguimiento/Seguimiento";
import Reportes from "./pages/reportes/Reportes";
import Anuncios from "./pages/anuncios/Anuncios";
import Material from "./pages/material/Material";
import AulaVirtual from "./pages/aula-virtual/AulaVirtual";
import AulaVirtualCurso from "./pages/aula-virtual/AulaVirtualCurso";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<PanelPrincipal />} />
        <Route path="/actividades" element={<Actividades />} />
        <Route path="/asistencia" element={<Asistencia />} />
        <Route path="/calificaciones" element={<Calificaciones />} />
        <Route path="/seguimiento" element={<Seguimiento />} />
        <Route path="/anuncios" element={<Anuncios />} />
        <Route path="/material" element={<Material />} />
        <Route path="/aula-virtual" element={<AulaVirtual />} />
        <Route path="/aula-virtual/:idAsignacion" element={<AulaVirtualCurso />} />
        <Route path="/reportes" element={<Reportes />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
