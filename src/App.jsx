import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Soporte  from "./pages/Soporte";
import Usuarios from "./pages/Usuarios";
import Dashboard from "./pages/Dashboard";
import { capturarTokenDeURL } from "./utils/auth";

function App() {
    // Al cargar la app, revisa si venimos de un login del principal
    // con el token en la URL, y si es asi lo guarda antes de renderizar rutas
    useEffect(() => {
        capturarTokenDeURL();
    }, []);

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/"          element={<Navigate to="/soporte" />} />
                <Route path="/soporte"   element={<Soporte />} />
                <Route path="/usuarios"  element={<Usuarios />} />
                <Route path="/dashboard" element={<Dashboard />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
