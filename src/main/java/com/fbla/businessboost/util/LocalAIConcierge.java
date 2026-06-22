package com.fbla.businessboost.util;

import com.fbla.businessboost.model.Business;

import java.util.*;

/**
 * Tier 2.2 - AI Concierge (Local Q&A).
 * Uses a lightweight TF-IDF (Term Frequency-Inverse Document Frequency)
 * and Cosine Similarity model to perform offline Natural Language Processing
 * without any external APIs.
 */
public class LocalAIConcierge {

    private static final List<String> STOP_WORDS = Arrays.asList(
            "the", "a", "an", "and", "or", "but", "is", "are", "was", "were",
            "in", "on", "at", "to", "for", "with", "about", "of", "it", "this", "that"
    );

    // Business -> Map of word -> TF-IDF score
    private Map<Business, Map<String, Double>> tfIdfVectors;
    private Map<String, Double> idfMap;
    private List<Business> corpus;

    public LocalAIConcierge(List<Business> businesses) {
        this.corpus = businesses;
        buildModel();
    }

    /**
     * Builds the TF-IDF vectors for all businesses offline.
     */
    private void buildModel() {
        tfIdfVectors = new HashMap<>();
        idfMap = new HashMap<>();
        
        List<List<String>> documentTokens = new ArrayList<>();
        Map<String, Integer> documentFrequency = new HashMap<>();

        // Tokenize all businesses
        for (Business b : corpus) {
            List<String> tokens = tokenize(
                    b.getName() + " " + b.getDescription() + " " + b.getCategory() + " " + String.join(" ", b.getTags())
            );
            documentTokens.add(tokens);

            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String t : uniqueTokens) {
                documentFrequency.put(t, documentFrequency.getOrDefault(t, 0) + 1);
            }
        }

        int n = corpus.size();
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            double idf = Math.log((double) n / (1 + entry.getValue()));
            idfMap.put(entry.getKey(), idf);
        }

        // Calculate TF-IDF vectors for businesses
        for (int i = 0; i < corpus.size(); i++) {
            Business b = corpus.get(i);
            List<String> tokens = documentTokens.get(i);
            Map<String, Double> tfIdf = calculateTfIdf(tokens);
            tfIdfVectors.put(b, tfIdf);
        }
    }

    private List<String> tokenize(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String w : words) {
            if (!w.isEmpty() && !STOP_WORDS.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private Map<String, Double> calculateTfIdf(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) {
            tf.put(t, tf.getOrDefault(t, 0) + 1);
        }

        Map<String, Double> tfIdf = new HashMap<>();
        int totalTokens = tokens.size();
        for (Map.Entry<String, Integer> entry : tf.entrySet()) {
            double termFreq = (double) entry.getValue() / totalTokens;
            double idf = idfMap.getOrDefault(entry.getKey(), Math.log(corpus.size() + 1));
            tfIdf.put(entry.getKey(), termFreq * idf);
        }
        return tfIdf;
    }

    /**
     * Searches the local model for the best matches to a natural language query.
     */
    public List<SearchResult> search(String query) {
        List<String> queryTokens = tokenize(query);
        Map<String, Double> queryVector = calculateTfIdf(queryTokens);

        List<SearchResult> results = new ArrayList<>();
        
        for (Business b : corpus) {
            Map<String, Double> docVector = tfIdfVectors.get(b);
            double similarity = cosineSimilarity(queryVector, docVector);
            
            if (similarity > 0.0) {
                // Find highest matching word for explanation
                String bestMatch = "";
                double bestMatchScore = -1;
                for (String qt : queryTokens) {
                    if (docVector.containsKey(qt) && docVector.get(qt) > bestMatchScore) {
                        bestMatchScore = docVector.get(qt);
                        bestMatch = qt;
                    }
                }
                
                String explanation = "Matched because of relevance to: '" + bestMatch + "'.";
                results.add(new SearchResult(b, similarity, explanation));
            }
        }

        // Sort descending by similarity score
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results;
    }

    private double cosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<String, Double> a : vecA.entrySet()) {
            normA += Math.pow(a.getValue(), 2);
            if (vecB.containsKey(a.getKey())) {
                dotProduct += a.getValue() * vecB.get(a.getKey());
            }
        }

        for (Double val : vecB.values()) {
            normB += Math.pow(val, 2);
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static class SearchResult {
        public Business business;
        public double score;
        public String explanation;

        public SearchResult(Business business, double score, String explanation) {
            this.business = business;
            this.score = score;
            this.explanation = explanation;
        }
    }
}
