import { useState, useEffect } from "react";
import { getMisAsignaciones, getActividadesPorAsignacion, createActividad, updateActividad, deleteActividad } from "../../services/docente/docenteService";

const PRIMARY = "#243A76";

export default function DocenteActividades() {
    const [asignaciones, setAsignaciones] = useState([]);
    const [asignacionSel, setAsignacionSel] = useState("");
    const [actividades, setActividades] = useState([]);
    const [loading, setLoading] = useState(false);

    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({
        id: null,
        titulo: "",
        descripcion: "",
        tipoActividad: "TAREA",
        fechaVencimiento: ""
    });

    useEffect(() => {
        cargarAsignaciones();
    }, []);

    useEffect(() => {
        if (asignacionSel) {
            cargarActividades(asignacionSel);
        } else {
            setActividades([]);
        }
    }, [asignacionSel]);

    const cargarAsignaciones = async () => {
        try {
            const data = await getMisAsignaciones();
            setAsignaciones(data);
        } catch (error) {
            console.error("Error cargando asignaciones:", error);
        }
    };

    const cargarActividades = async (asignacionId) => {
        try {
            setLoading(true);
            const data = await getActividadesPorAsignacion(asignacionId);
            setActividades(data);
        } catch (error) {
            console.error("Error cargando actividades:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleOpenModal = (act = null) => {
        if (act) {
            setFormData({
                id: act.id,
                titulo: act.titulo,
                descripcion: act.descripcion,
                tipoActividad: act.tipoActividad,
                fechaVencimiento: act.fechaVencimiento.substring(0, 16) // Format for datetime-local
            });
        } else {
            setFormData({
                id: null,
                titulo: "",
                descripcion: "",
                tipoActividad: "TAREA",
                fechaVencimiento: ""
            });
        }
        setShowModal(true);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        try {
            // Convert to format expected by backend (ISO-8601 without timezone 'Z' or formatted string)
            const payload = {
                asignacionId: parseInt(asignacionSel),
                titulo: formData.titulo,
                descripcion: formData.descripcion,
                tipoActividad: formData.tipoActividad,
                fechaVencimiento: new Date(formData.fechaVencimiento).toISOString()
            };

            if (formData.id) {
                await updateActividad(formData.id, payload);
            } else {
                await createActividad(payload);
            }
            setShowModal(false);
            cargarActividades(asignacionSel);
        } catch (error) {
            console.error("Error guardando actividad:", error);
            alert("Ocurrió un error al guardar la actividad.");
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("¿Está seguro de eliminar esta actividad?")) return;
        try {
            await deleteActividad(id);
            cargarActividades(asignacionSel);
        } catch (error) {
            console.error("Error eliminando actividad:", error);
            alert("Error al eliminar la actividad.");
        }
    };

    return (
        <div>
            <div className="flex justify-between items-end mb-6">
                <div>
                    <h1 className="text-2xl font-bold text-slate-800">Actividades</h1>
                    <p className="text-slate-500 text-sm mt-1">Gestione las tareas y evaluaciones de sus cursos.</p>
                </div>
                {asignacionSel && (
                    <button
                        onClick={() => handleOpenModal()}
                        style={{ backgroundColor: PRIMARY }}
                        className="text-white px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 transition shadow-sm flex items-center gap-2"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        Nueva Actividad
                    </button>
                )}
            </div>

            <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm mb-6">
                <label className="block text-sm font-semibold text-slate-700 mb-2">Seleccione su Curso / Asignatura:</label>
                <select
                    value={asignacionSel}
                    onChange={(e) => setAsignacionSel(e.target.value)}
                    className="w-full max-w-md p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:border-transparent text-sm"
                    style={{ '--tw-ring-color': PRIMARY }}
                >
                    <option value="">-- Seleccionar Asignatura --</option>
                    {asignaciones.map(a => (
                        <option key={a.idAsignacion} value={a.idAsignacion}>
                            {a.grado.nombre} - {a.asignatura.nombre}
                        </option>
                    ))}
                </select>
            </div>

            {asignacionSel ? (
                loading ? (
                    <div className="text-center py-10 text-slate-500">Cargando actividades...</div>
                ) : (
                    <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="bg-slate-50 border-b border-slate-200 text-xs text-slate-500 uppercase tracking-wider">
                                    <th className="px-6 py-3 font-semibold">Actividad</th>
                                    <th className="px-6 py-3 font-semibold">Tipo</th>
                                    <th className="px-6 py-3 font-semibold">Fecha Vencimiento</th>
                                    <th className="px-6 py-3 font-semibold text-right">Acciones</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200 text-sm">
                                {actividades.map((act) => (
                                    <tr key={act.id} className="hover:bg-slate-50 transition">
                                        <td className="px-6 py-4">
                                            <p className="font-semibold text-slate-800">{act.titulo}</p>
                                            <p className="text-xs text-slate-500 line-clamp-1">{act.descripcion}</p>
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className="bg-blue-100 text-blue-700 px-2 py-1 rounded text-xs font-medium">
                                                {act.tipoActividad}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-slate-600">
                                            {new Date(act.fechaVencimiento).toLocaleString()}
                                        </td>
                                        <td className="px-6 py-4 text-right flex items-center justify-end gap-2">
                                            <button onClick={() => handleOpenModal(act)} className="text-blue-500 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 p-1.5 rounded transition">
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" /></svg>
                                            </button>
                                            <button onClick={() => handleDelete(act.id)} className="text-red-500 hover:text-red-700 bg-red-50 hover:bg-red-100 p-1.5 rounded transition">
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                {actividades.length === 0 && (
                                    <tr>
                                        <td colSpan="4" className="px-6 py-10 text-center text-slate-500 text-sm">
                                            No hay actividades registradas para esta asignatura.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )
            ) : (
                <div className="text-center py-10 text-slate-500 bg-slate-50 rounded-xl border border-dashed border-slate-300">
                    Seleccione una asignatura para ver sus actividades.
                </div>
            )}

            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 px-4">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden">
                        <div style={{ backgroundColor: PRIMARY }} className="px-6 py-4 flex items-center justify-between text-white">
                            <h3 className="font-semibold text-lg">{formData.id ? "Editar" : "Nueva"} Actividad</h3>
                            <button onClick={() => setShowModal(false)} className="text-white hover:text-slate-200 transition">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                            </button>
                        </div>
                        <form onSubmit={handleSave} className="p-6">
                            <div className="mb-4">
                                <label className="block text-sm font-semibold text-slate-700 mb-1">Título</label>
                                <input
                                    type="text"
                                    required
                                    value={formData.titulo}
                                    onChange={e => setFormData({ ...formData, titulo: e.target.value })}
                                    className="w-full p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 text-sm"
                                    style={{ '--tw-ring-color': PRIMARY }}
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-semibold text-slate-700 mb-1">Descripción</label>
                                <textarea
                                    required
                                    rows="3"
                                    value={formData.descripcion}
                                    onChange={e => setFormData({ ...formData, descripcion: e.target.value })}
                                    className="w-full p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 text-sm"
                                    style={{ '--tw-ring-color': PRIMARY }}
                                ></textarea>
                            </div>
                            <div className="grid grid-cols-2 gap-4 mb-6">
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">Tipo</label>
                                    <select
                                        value={formData.tipoActividad}
                                        onChange={e => setFormData({ ...formData, tipoActividad: e.target.value })}
                                        className="w-full p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 text-sm"
                                        style={{ '--tw-ring-color': PRIMARY }}
                                    >
                                        <option value="TAREA">Tarea</option>
                                        <option value="LECCION">Lección</option>
                                        <option value="EXAMEN">Examen</option>
                                        <option value="PROYECTO">Proyecto</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">Fecha Vencimiento</label>
                                    <input
                                        type="datetime-local"
                                        required
                                        value={formData.fechaVencimiento}
                                        onChange={e => setFormData({ ...formData, fechaVencimiento: e.target.value })}
                                        className="w-full p-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 text-sm"
                                        style={{ '--tw-ring-color': PRIMARY }}
                                    />
                                </div>
                            </div>
                            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                                <button type="button" onClick={() => setShowModal(false)} className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition">Cancelar</button>
                                <button type="submit" style={{ backgroundColor: PRIMARY }} className="px-4 py-2 text-sm font-medium text-white hover:opacity-90 rounded-lg transition">
                                    Guardar
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
