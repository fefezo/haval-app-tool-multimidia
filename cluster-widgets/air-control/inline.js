var fs = require('fs');
var path = require('path');

// Função para processar um HTML e inlinear CSS/JS
function processHtml(htmlPath, outputPath) {
  console.log(`🔄 Processando: ${htmlPath}`);
  
  if (!fs.existsSync(htmlPath)) {
    console.log(`❌ Arquivo não encontrado: ${htmlPath}`);
    return;
  }

  var htmlContent = fs.readFileSync(htmlPath, 'utf8');

  // Inline CSS
  var cssRegex = /<link[^>]*href=([^>\s]+\.css)[^>]*>/g;
  var cssMatch;
  while ((cssMatch = cssRegex.exec(htmlContent)) !== null) {
    var cssPath = cssMatch[1].replace(/['"]/g, '');
    
    // Converte caminho relativo para absoluto
    var fullCssPath;
    if (cssPath.startsWith('/src/')) {
      fullCssPath = path.join(__dirname, cssPath.substring(1));
    } else {
      fullCssPath = path.join(__dirname, 'dist', cssPath);
    }
    
    if (fs.existsSync(fullCssPath)) {
      var cssContent = fs.readFileSync(fullCssPath, 'utf8');
      htmlContent = htmlContent.replace(cssMatch[0], function () { return '<style>' + cssContent + '</style>'; });
      console.log('✅ CSS inlined:', cssPath);
    }
  }

  // Inline JavaScript
  var jsRegex = /<script[^>]*src=([^>\s]+\.js)[^>]*><\/script>/g;
  var jsMatch;
  while ((jsMatch = jsRegex.exec(htmlContent)) !== null) {
    var jsPath = jsMatch[1].replace(/['"]/g, '');
    
    var fullJsPath = path.join(__dirname, 'dist', jsPath);
    
    if (fs.existsSync(fullJsPath)) {
      var jsContent = fs.readFileSync(fullJsPath, 'utf8');
      // v2.4: escapa </script> cru. Um </script> dentro do JS faria o parser HTML
      // fechar o bloco no meio do código — SyntaxError silencioso que matou o
      // widget da v2.0 à v2.3 (app.html embutido com tags <script> cruas).
      jsContent = jsContent.replace(/<\/script>/gi, '<\\/script>');
      // Defesa 1: se o conteúdo ainda tiver tag <script>, o bundle está contaminado
      // (cache/dist sujos do parcel) — aborta em vez de gerar APK quebrado.
      if (/<script/gi.test(jsContent)) {
        console.error('❌ JS contém tag <script crua — bundle contaminado. Limpe dist/ e .parcel-cache e rebuild (npm run build:night).');
        process.exit(1);
      }
      // Defesa 2: checagem de sintaxe real sem executar — um SyntaxError aqui
      // chegaria ao APK como widget morto invisível no logcat.
      try {
        new Function(jsContent);
      } catch (e) {
        console.error('❌ JS inlineado tem erro de sintaxe: ' + e.message + ' — abortando.');
        process.exit(1);
      }
      // ROOT CAUSE v2.0-v2.3 (provado): replace com STRING como 2º argumento
      // expande padrões $&, $', $`, $$ do conteúdo minificado — o bundle do
      // parcel contém $& (substituições de regex do Chart.js/state), e cada
      // $& vira o texto do match (a própria tag <script src=...>) no meio do
      // JS → parser HTML fecha o <script> ali → SyntaxError → widget morto
      // silencioso. O arquivo do bundle SEMPRE esteve limpo; a corrupção era
      // gerada em memória por este replace. Função de substituição NÃO
      // expande padrões $ — conteúdo literal, byte a byte.
      htmlContent = htmlContent.replace(jsMatch[0], function () { return '<script>' + jsContent + '</script>'; });
      fs.unlinkSync(fullJsPath);
      console.log('✅ JS inlined:', jsPath);
    }
  }

  // Verificação final (v2.4): o HTML embutido não pode ter tags <script> além
  // do wrapper único — tags cruas dentro do JS = widget morto silencioso.
  var scriptTags = (htmlContent.match(/<script/gi) || []).length;
  if (scriptTags !== 1) {
    console.error('❌ HTML final tem ' + scriptTags + ' tags <script> (esperado 1) — abortando. Limpe dist/ e .parcel-cache e rebuild.');
    process.exit(1);
  }

  // Salva o HTML processado
  fs.writeFileSync(outputPath, htmlContent, 'utf8');
  console.log(`✅ HTML gerado: ${outputPath}`);
}

// Processa os dois temas
console.log('🚀 Iniciando build dos temas...');

// Night theme
var nightHtmlPath = path.join(__dirname, 'dist', 'app-night.html');
var nightOutputPath = path.join(__dirname, 'dist', 'app-night.html');
processHtml(nightHtmlPath, nightOutputPath);

// Light theme  
var lightHtmlPath = path.join(__dirname, 'dist', 'app-light.html');
var lightOutputPath = path.join(__dirname, 'dist', 'app-light.html');
processHtml(lightHtmlPath, lightOutputPath);

// Remove pasta assets vazia
var assetsDir = path.join(__dirname, 'dist', 'assets');
if (fs.existsSync(assetsDir)) {
  var files = fs.readdirSync(assetsDir);
  if (files.length === 0) {
    fs.rmdirSync(assetsDir);
    console.log('✅ Pasta assets removida');
  }
}

// Remove arquivos CSS originais
var cssFiles = ['night.style.css', 'light.style.css'];
cssFiles.forEach(function(cssFile) {
  var cssPath = path.join(__dirname, 'dist', cssFile);
  if (fs.existsSync(cssPath)) {
    fs.unlinkSync(cssPath);
    console.log(`✅ CSS removido: ${cssFile}`);
  }
});

console.log('🎉 Build completo! Arquivos gerados:');
console.log('  📄 app-night.html (tema escuro)');
console.log('  📄 app-light.html (tema claro)');
