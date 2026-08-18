import { createContext, useContext, useState, useCallback } from "react";
import ConfirmModal from "../components/ConfirmModal";

const ConfirmContext = createContext(null);

export function ConfirmProvider({ children }) {
  const [config, setConfig] = useState(null);

  const confirm = useCallback((options) => {
    return new Promise((resolve) => {
      setConfig({
        title: options.title || "¿Estás seguro?",
        message: options.message || "Esta acción no se puede deshacer.",
        confirmText: options.confirmText || "Sí, proceder",
        cancelText: options.cancelText || "Cancelar",
        type: options.type || "danger", // danger | warning | info
        resolve,
      });
    });
  }, []);

  const handleConfirm = () => {
    if (config?.resolve) config.resolve(true);
    setConfig(null);
  };

  const handleCancel = () => {
    if (config?.resolve) config.resolve(false);
    setConfig(null);
  };

  return (
    <ConfirmContext.Provider value={{ confirm }}>
      {children}
      {config && (
        <ConfirmModal
          config={config}
          onConfirm={handleConfirm}
          onCancel={handleCancel}
        />
      )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const context = useContext(ConfirmContext);
  if (!context) {
    throw new Error("useConfirm debe ser usado dentro de un ConfirmProvider");
  }
  return context.confirm;
}
