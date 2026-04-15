package com.berdachuk.docurag.evaluation.internal;

import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Profile("eval-cli")
@Order(Integer.MAX_VALUE)
@RequiredArgsConstructor
public class EvalCliRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalCliRunner.class);

    private final EvaluationApi evaluationApi;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        String dataset = firstOption(args, "dataset", "medical-rag-eval-v1");
        int topK = optArg(args, "topK").map(Integer::parseInt).orElse(5);
        double minScore = optArg(args, "minScore").map(Double::parseDouble).orElse(0.5);
        double sem = optArg(args, "semanticPassThreshold").map(Double::parseDouble).orElse(0.8);
        log.info("Running evaluation for dataset {}", dataset);
        var out = evaluationApi.run(new EvaluationRunRequest(dataset, topK, minScore, sem));
        log.info("Finished run {} — normalizedAcc={} meanSem={} sem080={}",
                out.runId(), out.normalizedAccuracy(), out.meanSemanticSimilarity(), out.semanticAccuracyAt080());
        int code = SpringApplication.exit(context, () -> 0);
        System.exit(code);
    }

    private static String firstOption(ApplicationArguments args, String name, String defaultValue) {
        List<String> v = args.getOptionValues(name);
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        return v.getFirst();
    }

    private static Optional<String> optArg(ApplicationArguments args, String name) {
        List<String> v = args.getOptionValues(name);
        if (v == null || v.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(v.getFirst());
    }
}
