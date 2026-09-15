/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_WS_BASE_URL?: string;
  readonly VITE_EMAIL_VERIFICATION_REQUIRED?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
