import { defineConfig } from 'orval';

export default defineConfig({
  platform: {
    input: {
      target: './src/api/openapi.json',
    },
    output: {
      target: './src/api/generated/endpoints.ts',
      schemas: './src/api/generated/types',
      client: 'axios',
      mode: 'tags-split',
      override: {
        mutator: {
          path: './src/api/client/apiMutator.ts',
          name: 'apiMutator',
        },
      },
    },
  },
});