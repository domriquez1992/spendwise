/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        // Inter for UI, Fraunces for the brand wordmark and a few display moments.
        sans: ['"Inter Variable"', 'system-ui', 'sans-serif'],
        display: ['"Fraunces Variable"', 'Georgia', 'serif'],
      },
      colors: {
        // A calm private-banking palette. Semantic token names so the components read
        // intentionally (bg-surface, text-muted, bg-brand) rather than by raw hue.
        ink: {
          DEFAULT: '#15201c', // primary text
          soft: '#3c4b45',
        },
        muted: '#6b7a73', // secondary text
        canvas: '#f6f4ee', // app background
        surface: '#fffefb', // raised cards and inputs
        sunken: '#eceae1', // subtle wells / table header
        line: '#dcd9cd', // hairline borders
        brand: {
          DEFAULT: '#0f6b5c', // teal accent
          dark: '#0a4e43',
          soft: '#2f8a79',
          wash: '#e4efe9',
        },
        alert: {
          DEFAULT: '#b3261e', // destructive actions and validation
          soft: '#fbeae8',
        },
      },
      borderRadius: {
        xl: '0.875rem',
      },
    },
  },
  plugins: [],
}
