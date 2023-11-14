/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.cljs"],
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

