import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "./",
  build: {
    outDir: "../target/frontend",
    assetsDir: "assets/react",
    emptyOutDir: true,
  },
  server: {
    proxy: {
      "/hospital/api": "http://127.0.0.1:3000",
      "/hospital/attachments": "http://127.0.0.1:3000",
      "/hospital/clinical-orders": "http://127.0.0.1:3000",
      "^/hospital/.*[?&](format|fileRequestId|originalRequestId)=":
        "http://127.0.0.1:3000",
      "/hospital/payment": "http://127.0.0.1:3000",
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test-setup.js",
  },
});
