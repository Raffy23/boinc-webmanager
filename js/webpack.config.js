require('webpack');

const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CompressionPlugin = require('compression-webpack-plugin');
const scalaJSBundlerConfig = require('./scalajs.webpack.config');

/** @type {("development"|"production")} */
const buildMode = scalaJSBundlerConfig.mode;

const common = {
  ...scalaJSBundlerConfig,

  performance: { hints: false },

  output: {
    ...scalaJSBundlerConfig.output,
    hashFunction: 'sha256'
  },

  module: {
    ...scalaJSBundlerConfig.module,
    rules: [
      ...scalaJSBundlerConfig.module.rules,
      {
        test: /\.css$/i,
        use: [MiniCssExtractPlugin.loader, 'css-loader', 'postcss-loader'],
      },
      {
        test: /\.(png|jpg|woff|woff2|ttf|eot|svg|txt)$/i,
        type: 'asset/resource'
      }
    ]
  },
}

const dev = {
  ...common,
  devtool: 'cheap-module-source-map',
  plugins: [
    new MiniCssExtractPlugin({
      filename: '[name]-bundle.css',
    })
  ],
}

const prod = {
  ...common,
  plugins: [
    new MiniCssExtractPlugin({
      filename: '[name]-bundle.css',
    }),
    new CompressionPlugin({
      test: /\.(js|css|html|svg|json|woff|woff2)$/,
      deleteOriginalAssets: false,
    }),
    new CompressionPlugin({
      test: /\.(js|css|html|svg|woff|woff2)$/,
      filename: '[path][base].br',
      algorithm: 'brotliCompress',
      compressionOptions: {
        // zlib’s `level` option matches Brotli’s `BROTLI_PARAM_QUALITY` option.
        level: 11,
      },
      minRatio: 0.8,
      deleteOriginalAssets: false,
    })
  ]
}

// And then modify `module.exports` to extend the configuration
module.exports = buildMode === "development" ? dev : prod;
