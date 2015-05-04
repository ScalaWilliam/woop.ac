var gulp = require('gulp'),
    less = require('gulp-less'),
    autoprefixer = require('gulp-autoprefixer'),
    sourcemaps = require('gulp-sourcemaps'),
    handleErrors = require('../util/handleErrors'),
    browserSync = require('browser-sync'),
    sass        = require('gulp-sass'),
    reload      = browserSync.reload,
    config      = require('../config');


gulp.task('build', ['browserify', 'markup', 'less', 'sass']);

gulp.task('default', ['watch']);

gulp.task('less', function() {
    return gulp.src(config.less.src)
        .pipe(sourcemaps.init())
        .pipe(less())
        .on('error', handleErrors)
        .pipe(autoprefixer({cascade: false, browsers: ['last 2 versions']}))
        .pipe(sourcemaps.write())
        .pipe(gulp.dest(config.less.dest));
});

gulp.task('markup', function() {
    return gulp.src(config.markup.src)
        .pipe(gulp.dest(config.markup.dest));
});

gulp.task('sass', function () {
    gulp.src(config.sass.src)
        .pipe(sourcemaps.init())
        .pipe(sass(config.sass.includePaths))
        .on('error', handleErrors)
        .pipe(sourcemaps.write())
        .pipe(gulp.dest(config.sass.dest));
    //.pipe(reload({stream: true}));
});

gulp.task('setWatch', function() {
    global.isWatching = true;
});

gulp.task('watch', ['setWatch', 'browserSync'], function() {
    gulp.watch(config.less.watch, ['less']);
    gulp.watch(config.sass.watch, ['sass']);
    gulp.watch(config.markup.src, ['markup']);
});
