package org.njupt.util;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;


public class DzyUtils {

    public static final Logger logger = LoggerFactory.getLogger(DzyUtils.class);


    /**
     * Tokenize code line (use javaParser)
     * @param codeLine code line String
     * @return tokens list
     */
    public static List<String> tokenize(String codeLine){

        List<String> tokenList  = new ArrayList<>();
        //Firstly, need replace "\n" with "NewLineDZY" to rebuild line-level conflict
        codeLine = codeLine.replace("\n", " NewLineDZY ");

        StringProvider provider = new StringProvider(codeLine);
        SimpleCharStream charStream = new SimpleCharStream(provider);
        GeneratedJavaParserTokenManager tokenGenerate = new GeneratedJavaParserTokenManager(charStream);
        String strToken = tokenGenerate.getNextToken().toString();
        while (!strToken.equals("")){
            tokenList.add(strToken);
            strToken = tokenGenerate.getNextToken().toString();
        }
        return tokenList;
    }

    /**
     * New Tokenizer (use javaParser) 2023年11月5日19:35:14
     * @param code code lines
     * @return tokens list
     */
    public static List<String> newTokenizer(String code){

        List<String> tokenList  = new ArrayList<>();
        //Firstly, need replace "\n" with "NewLineDZY" to rebuild line-level conflict
        String newCode = code.replace("\n", "\n NewLineDZY ");

        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
        Optional<CompilationUnit> compilationUnit = javaParser.parse(newCode).getResult();
        if (compilationUnit.isPresent()) {
            TokenRange tokenRange = compilationUnit.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    tokenList.add(token.getText());
                }
            });
        } else {
            logger.error("Code not parsed correctly! ***************************************************Try to use Unicode");
            return DzyUtils.tokenizeUnicode(code);
        }
        return tokenList;
    }
    public static String newTokenizerToString(String code){
        StringBuilder tokenString = new StringBuilder();
        JavaParser javaParser = new JavaParser(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver())));
        Optional<CompilationUnit> compilationUnit = javaParser.parse(code).getResult();
        if (compilationUnit.isPresent()) {
            TokenRange tokenRange = compilationUnit.get().getTokenRange().get();
            tokenRange.forEach(token -> {
                if (!Objects.equals(token.getText().replaceAll("\\s*",""), "")){
                    tokenString.append(token.getText()).append(" ");
                }
            });
        } else {
//            logger.error("Code not parsed correctly! ***************************************************Try to use Unicode");
            return DzyUtils.tokenizeUnicodeToString(code);
        }
        return tokenString.toString();
    }

    /**
     * Unicode tokenize code(with comment)
     * @param codeComment code with comment(javaParser can't have use)
     * @return list
     */
    public static List<String> tokenizeUnicode(String codeComment){
        codeComment = codeComment.replace("\n", " NewLineDZY ");
        String regex = "[\\p{L}\\p{M}\\p{N}]+(?:\\p{Pi}[\\p{L}\\p{M}\\p{N}]+)*|[\\p{P}\\p{S}]";
        String[] parts = Pattern.compile(regex).matcher(codeComment).results().map(MatchResult::group).toArray(String[]::new);
        return List.of(parts);
    }
    public static String tokenizeUnicodeToString(String codeComment){
        String regex = "[\\p{L}\\p{M}\\p{N}]+(?:\\p{Pi}[\\p{L}\\p{M}\\p{N}]+)*|[\\p{P}\\p{S}]";
        String[] parts = Pattern.compile(regex).matcher(codeComment).results().map(MatchResult::group).toArray(String[]::new);
        StringBuilder tokenString = new StringBuilder();
        for (String part : parts){
            tokenString.append(part).append(" ");
        }
        return tokenString.toString();
    }

    /**
     * According to String, write in file
     * @param filePath file's path
     * @param context file's context
     */
    public static void stringToBuildFile(String filePath, String context){
        Path path = Paths.get(filePath);

        // Check if the parent directory exists, if not, create it
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create parent directory: " + parentDir, e);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Official Code perfect matching rate.
     * @param ours ours code
     * @param target target code
     * @return percent of match rate
     */
    public static Double perfectMatchRate(String ours, String target){
        ours = ours.replaceAll("\\s*","");
        target = target.replaceAll("\\s*","");
        if (ours.length() == 0 && target.length() == 0) return 100.0;

        //1.Use Apache Commons Text(Jaccard Similarity)
//        JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
//        double similarity = jaccardSimilarity.apply(ours, target);

        //2.Use Apache Commons Text(Levenshtein Similarity)
        int distance = LevenshteinDistance.getDefaultInstance().apply(ours, target);
        double similarity = 1 - (double) distance / Math.max(ours.length(), target.length());

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        logger.info("Code's similarity is : {}%", decimalFormat.format(similarity * 100));
        return Double.valueOf(decimalFormat.format(similarity * 100));
    }

    /**
     * Take token list to new file
     * @param filePath new file path
     * @param tokenList token list
     */
    public static void tokenListToNewFile(String filePath, List<String> tokenList){
        Path path = Paths.get(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (int i = 0; i < tokenList.size(); i++){
                writer.write(tokenList.get(i));
                if (i != tokenList.size() - 1) writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Count the number of times a character appears in the list
     * @param target target token
     * @param list token list
     * @return count
     */
    public static int tokenCountInList(String target, List<String> list){
        int count = 0;
        for (String token : list){
            if (Objects.equals(target, token)) count++;
        }
        return count;
    }

    public static void tokenLevelMerge(String a_contents, String o_contents, String b_contents, String commonPath) throws IOException {

        List<String> listA = DzyUtils.newTokenizer(a_contents);
        List<String> listO = DzyUtils.newTokenizer(o_contents);
        List<String> listB = DzyUtils.newTokenizer(b_contents);

        //2.Define temp file path and Build new file
        String aPath = commonPath+ "_A.txt";
        String oPath = commonPath + "_O.txt";
        String bPath = commonPath + "_B.txt";
        String mergedPath = commonPath + "_merged.txt";
        DzyUtils.tokenListToNewFile(aPath, listA);//A
        DzyUtils.tokenListToNewFile(oPath, listO);//O
        DzyUtils.tokenListToNewFile(bPath, listB);//B

        //3.Start use Diff3 merge A O B to generate Token-level conflicts
        File a = new File(aPath);
        File o = new File(oPath);
        File b = new File(bPath);
        File merged = new File(mergedPath);
        if(merged.exists()) merged.delete();
        Files.copy(a.toPath(), merged.toPath());
        ProcessBuilder pb = new ProcessBuilder(
                "git",
                "merge-file",
                "--diff3",
                merged.getPath(),
                o.getPath(),
                b.getPath());
        try {
            pb.start().waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //4.Restoring conflicting blocks of token-level to row line-level construction
        FileReader fileReader = new FileReader(merged);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder fileContext = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("<<<<<<<") || line.startsWith("|||||||") || line.startsWith("=======") || line.startsWith(">>>>>>>")){
                fileContext.append("\n");
                fileContext.append(line);
                fileContext.append("\n");
            }else {
                fileContext.append(line + " ");
            }
        }
        String tokenMergeResult = fileContext.toString().replace("NewLineDZY", "\n");
        String lineMergedPath = commonPath + "_lineMerged.txt";
        Path path = Paths.get(lineMergedPath);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            //write in Line-Merged file
            writer.write(tokenMergeResult);//Restore special token
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //5.Delete temp file(A O B merged)
        a.delete();
        o.delete();
        b.delete();
        logger.info("Succeed git merge-file --diff3 {}", lineMergedPath);
    }
    private static String addBlank(String target){
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < target.length(); i++){
            if (target.charAt(i) == ' '){
                result.append(" blank ");
            }else {
                result.append(target.charAt(i)).append(i).append(" ");
            }
        }
        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        String a_contents = "defaultLoggingSink ( ) , null ";
        String o_contents = "defaultLoggingSink ( ) ";
        String b_contents = "log :: info ";

        tokenLevelMerge(addBlank(a_contents), addBlank(o_contents), addBlank(b_contents), "G:\\now\\2024merge\\TestTest1207\\a");



//        System.out.println(DzyUtils.newTokenizerToString("eventsRepository.syncTagsFilter(historyManagerParams.getFilter().getTagsFilter());\n"));
//        System.out.println(DzyUtils.tokenizeUnicodeToString("eventsRepository.syncTagsFilter(historyManagerParams.getFilter().getTagsFilter());\n"));
    }

}
