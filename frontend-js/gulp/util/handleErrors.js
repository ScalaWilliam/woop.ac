var notify = require("gulp-notify");

module.exports = function() {

  var args = Array.prototype.slice.call(arguments);

  console.log("Compile error", args);

  // Keep gulp from hanging on this task
  this.emit('end');
};
