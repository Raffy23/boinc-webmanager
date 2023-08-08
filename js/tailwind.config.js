const colors = require('tailwindcss/colors')
const scalaVersion = require('./scala-version')

module.exports = (api) => {
  /** @type {("fastopt"|"opt")} */
  const scalajsMode = api.mode === 'production' ? 'opt' : 'fastopt'

  /** @type {import('tailwindcss').Config} */
  return {
    mode: 'jit',
    content: [
      `./boinc-webmanager_client-${scalajsMode}.js`,
    ],
    theme: {
      extend: {
        fontFamily: {
          condensed: [
            'Roboto Condensed'
          ]
        },
      }
    },
    plugins: [
      require('@tailwindcss/forms'),
      require('@tailwindcss/typography'),
      require('@tailwindcss/aspect-ratio')
    ],
  }
}
