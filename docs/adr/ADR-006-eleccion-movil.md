# ADR-006: Elección de la Pila Tecnológica para la Aplicación Móvil

## Estado
Aceptado

## Contexto
La Guía E4 (Módulo C) exige la justificación cuantitativa de la tecnología seleccionada para el cliente móvil institucional del Representante, considerando requisitos de rendimiento, consumo de memoria, autenticación biométrica y sincronización offline.

## Decisión
Se seleccionó **Android Nativo con Kotlin y Jetpack Compose** evaluando criterios cuantitativos:
1. **Rendimiento de Renderizado:** Tasa de refresco estable a 60 fps con Jetpack Compose frente al sobrecosto de puentes JS/híbridos.
2. **Consumo de Memoria RAM:** Huella de memoria promedio $\approx 45\text{ MB}$ frente a $>110\text{ MB}$ en frameworks basados en WebView/Electron.
3. **Acceso Nativo a Hardware:** Integración directa con `BiometricPrompt` (huella/rostro), `NotificationManager` (Push) y `Room Database` con `WorkManager` para sincronización offline con cola de reintentos.

## Consecuencias
- Máxima fluidez y seguridad al consultar información sensible de menores de edad.
- Arquitectura desacoplada MVVM con repositorio local y sincronización en segundo plano.
