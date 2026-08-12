/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fef2f2',
          100: '#ffe1e1',
          200: '#ffc7c7',
          300: '#ffa0a0',
          400: '#ff6b6b',
          500: '#f83b3b',
          600: '#e51d1d',
          700: '#c11414',
          800: '#a01414',
          900: '#841818',
          950: '#480707'
        },
        ink: {
          50: '#f4f7f7',
          100: '#e2eaea',
          200: '#c5d5d6',
          300: '#9db6b8',
          400: '#6f9093',
          500: '#547478',
          600: '#486065',
          700: '#3e5055',
          800: '#374448',
          900: '#313b3e',
          950: '#1d2528'
        }
      },
      fontFamily: {
        display: ['"Source Serif 4"', 'Georgia', 'serif'],
        sans: ['"DM Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif']
      },
      backgroundImage: {
        'hero-mesh': 'radial-gradient(ellipse at top left, rgba(248,59,59,0.18), transparent 45%), radial-gradient(ellipse at bottom right, rgba(61,80,85,0.12), transparent 40%), linear-gradient(160deg, #fff8f6 0%, #f3f7f7 45%, #eef3f4 100%)'
      }
    }
  },
  plugins: []
};
