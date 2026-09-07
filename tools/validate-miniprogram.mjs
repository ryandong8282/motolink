import { readFileSync, readdirSync, statSync } from 'node:fs';
import { extname, join, relative, resolve } from 'node:path';

const repositoryRoot = resolve(import.meta.dirname, '..');
const projectRoot = join(repositoryRoot, 'apps', 'miniprogram');
const miniprogramRoot = join(projectRoot, 'miniprogram');

const files = walk(projectRoot);
let checkedJson = 0;

for (const file of files) {
  if (extname(file) !== '.json') continue;
  try {
    JSON.parse(readFileSync(file, 'utf8'));
    checkedJson += 1;
  } catch (error) {
    throw new Error(`${relative(repositoryRoot, file)} 不是合法 JSON: ${error.message}`);
  }
}

const appConfig = JSON.parse(readFileSync(join(miniprogramRoot, 'app.json'), 'utf8'));
for (const page of appConfig.pages || []) {
  for (const extension of ['.js', '.json', '.wxml', '.wxss']) {
    const expected = join(miniprogramRoot, `${page}${extension}`);
    if (!files.includes(expected)) {
      throw new Error(`app.json 声明的页面缺少文件: ${relative(repositoryRoot, expected)}`);
    }
  }
}

console.log(`Mini Program structure OK (${checkedJson} JSON files, ${appConfig.pages.length} pages).`);

function walk(directory) {
  return readdirSync(directory)
    .flatMap((name) => {
      const fullPath = join(directory, name);
      return statSync(fullPath).isDirectory() ? walk(fullPath) : [fullPath];
    });
}
