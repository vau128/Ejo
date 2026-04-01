/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef4ff',
          100: '#dfe9ff',
          300: '#8fb3ff',
          500: '#4b7bff',
          600: '#3466f6',
          700: '#274fc5',
        },
      },
      boxShadow: {
        soft: '0 12px 30px rgba(15, 23, 42, 0.06)',
      },
    },
  },
  plugins: [],
};
