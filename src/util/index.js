/**
 * Copyright (c) 2020 August
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/** Object of utilities avaliable */
module.exports = {
  /**
   * Gets the latest commit hash
   * @returns {string} A string of the hash or `null` if it can't find the .git folder
   */
  getCommitHash: () => {
    const hash = execSync('git rev-parse HEAD', { encoding: 'utf8' });
    return hash.slice(0, 8);
  },

  /**
   * Joins the current directory with any other appending directories
   * @param {...string} paths The paths to conjoin
   * @returns {string} The paths conjoined 
   */
  getPath(...paths) {
    const sep = process.platform === 'win32' ? '\\' : '/';
    return `${process.cwd()}${sep}${paths.join(sep)}`;
  },

  /**
   * Returns the seperator for Windows/Unix systems
   */
  sep: process.platform === 'win32' ? '\\' : '/',

  /**
   * If the OS is running Node.js 10 or higher
   */
  isNode10: () => {
    const ver = process.version.split('.')[0].replace('v', '');
    const num = Number(ver);

    return num === 10 || num > 10;
  }
};