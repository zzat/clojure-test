/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./cljs-src/**/*.cljs"],
  theme: {
    fontFamily: {
      "sans": ['"Lato", sans-serif'],
    },
    extend: {},
  },
  plugins: [
    require('@tailwindcss/typography'),
    require('@tailwindcss/forms'),
    require('@tailwindcss/aspect-ratio'),
  ],
}

