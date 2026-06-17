/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_CURRENCY?: string
}

// Fontsource variable-font packages ship CSS only (no type declarations). These are
// side-effect imports, so declare them as modules to satisfy noUncheckedSideEffectImports.
declare module '@fontsource-variable/inter'
declare module '@fontsource-variable/fraunces'
