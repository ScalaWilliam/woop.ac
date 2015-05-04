var dest = './build',
  src = './src',
  mui = './node_modules/material-ui/src';

module.exports = {
  browserSync: {
    server: {
      // We're serving the src folder as well
      // for sass sourcemap linking
      baseDir: [dest, src]
    },
    files: [
      dest + '/**'
    ]
  },
  less: {
    src: src + '/mainl.less',
    watch: [
      src + '/*.less',
      mui + '/less/**'
    ],
    dest: dest
  },
  sass: {
    src: src + '/mains.sass',
    watch: [
      src + '/*.sass'
    ],
    dest: dest,
    includePaths: ['scss', 'bower_components/material-color']
  },
  markup: {
    src: [src + "/**.html", src + "/**.jsx"],
    dest: dest
  },
  browserify: {
    // Enable source maps
    debug: true,
    // A separate bundle will be generated for each
    // bundle config in the list below
    bundleConfigs: [{
      entries: src + '/app.jsx',
      dest: dest,
      outputName: 'app.js'
    },
      {
        entries: src + '/whut.jsx',
        dest: dest,
        outputName: 'whut.js'
      }
    ]
  }
};
