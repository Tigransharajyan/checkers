// Optional asset rebuild; the resulting bundle is committed, so Maven needs no Node runtime.
// npm install --prefix /tmp/checkers-icons lucide@0.468.0 esbuild@0.25.0
// node scripts/build-icons.cjs /tmp/checkers-icons
const fs = require('node:fs');
const path = require('node:path');
const toolsRoot = process.argv[2];
if (!toolsRoot) throw new Error('Pass the directory containing the optional build dependencies.');
const esbuild = require(require.resolve('esbuild', { paths: [toolsRoot] }));
const lucideRoot = path.resolve(path.dirname(require.resolve('lucide', { paths: [toolsRoot] })), '../..');
const resources = path.resolve(__dirname, '../src/main/resources');
function files(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry =>
    entry.isDirectory() ? files(path.join(directory, entry.name)) : [path.join(directory, entry.name)]);
}
const names = new Set();
for (const file of [...files(path.join(resources, 'templates')), ...files(path.join(resources, 'static/js')).filter(file => !file.includes('/vendor/'))]) {
  for (const match of fs.readFileSync(file, 'utf8').matchAll(/data-lucide=["']([a-z0-9-]+)["']/g)) names.add(match[1]);
}
const icons = [...names].sort().map(name => name.split('-').map(part => part[0].toUpperCase() + part.slice(1)).join(''));
const entry = `import {createIcons, ${icons.join(',')}} from ${JSON.stringify(path.join(lucideRoot, 'dist/esm/lucide.js'))};\nwindow.lucide={createIcons:()=>createIcons({icons:{${icons.join(',')}}})};`;
esbuild.buildSync({
  stdin: { contents: entry, resolveDir: resources }, bundle: true, minify: true,
  target: 'es2020', format: 'iife', legalComments: 'inline',
  outfile: path.join(resources, 'static/js/vendor/icons-0.468.0.min.js')
});
fs.copyFileSync(path.join(lucideRoot, 'LICENSE'), path.join(resources, 'static/js/vendor/lucide-LICENSE.txt'));
console.log(`Bundled ${icons.length} icons, preserving Lucide's official renderer.`);
