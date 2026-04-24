/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#F2F7F5',
          100: '#D8E6E2',
          300: '#9FBDB7',
          500: '#5D847F',
          600: '#4D706C',
          700: '#3E5C58',
        },
      },
      boxShadow: {
        soft: '0 12px 30px rgba(15, 23, 42, 0.06)',
      },
    },
  },
  plugins: [],
};
