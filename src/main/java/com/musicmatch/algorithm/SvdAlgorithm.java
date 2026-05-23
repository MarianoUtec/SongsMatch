package com.musicmatch.algorithm;

import com.musicmatch.entity.Rating;
import com.musicmatch.exception.SvdComputationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class SvdAlgorithm {

    private static final int K = 3; // dimensions to keep
    private static final int MIN_RATINGS_REQUIRED = 2;

    public record SvdResult(
        double[][] U,        // users in latent space [users x k]
        double[] sigma,      // singular values [k]
        double[][] Vt,       // songs in latent space [k x songs]
        List<Long> userIds,
        List<Long> songIds
    ) {}

    public SvdResult compute(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            throw new SvdComputationException("No ratings available for SVD computation");
        }

        // Build ordered lists of unique users and songs
        List<Long> userIds = ratings.stream()
            .map(r -> r.getUser().getId()).distinct().sorted().toList();
        List<Long> songIds = ratings.stream()
            .map(r -> r.getSong().getId()).distinct().sorted().toList();

        if (userIds.size() < MIN_RATINGS_REQUIRED) {
            throw new SvdComputationException("Need at least " + MIN_RATINGS_REQUIRED + " users with ratings");
        }

        // Build ratings matrix A [users x songs], fill missing with 0
        int m = userIds.size();
        int n = songIds.size();
        double[][] A = new double[m][n];

        Map<Long, Integer> userIndex = new HashMap<>();
        Map<Long, Integer> songIndex = new HashMap<>();
        for (int i = 0; i < userIds.size(); i++) userIndex.put(userIds.get(i), i);
        for (int j = 0; j < songIds.size(); j++) songIndex.put(songIds.get(j), j);

        for (Rating r : ratings) {
            int i = userIndex.get(r.getUser().getId());
            int j = songIndex.get(r.getSong().getId());
            A[i][j] = r.getScore();
        }

        // Apply SVD using Apache Commons Math
        try {
            RealMatrix matrix = MatrixUtils.createRealMatrix(A);
            SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

            int k = Math.min(K, Math.min(m, n));

            // U: [m x k] — users in latent space
            double[][] fullU = svd.getU().getData();
            double[][] U = new double[m][k];
            for (int i = 0; i < m; i++)
                for (int j = 0; j < k; j++)
                    U[i][j] = fullU[i][j];

            // Sigma: first k singular values
            double[] fullSigma = svd.getSingularValues();
            double[] sigma = Arrays.copyOf(fullSigma, k);

            // Vt: [k x n] — songs in latent space
            double[][] fullVt = svd.getVT().getData();
            double[][] Vt = new double[k][n];
            for (int i = 0; i < k; i++)
                for (int j = 0; j < n; j++)
                    Vt[i][j] = fullVt[i][j];

            log.info("SVD computed: {}x{} matrix → k={} dimensions, σ1={:.2f}",
                m, n, k, sigma[0]);

            return new SvdResult(U, sigma, Vt, userIds, songIds);

        } catch (Exception e) {
            throw new SvdComputationException("SVD computation failed: " + e.getMessage());
        }
    }

    /**
     * Cosine similarity between two vectors. Returns value in [-1, 1].
     * Closer to 1 = more similar.
     */
    public double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Find the index of the most similar user to targetIndex in U matrix.
     */
    public int findClosestUserIndex(double[][] U, int targetIndex) {
        double[] target = U[targetIndex];
        double bestSimilarity = -2;
        int bestIndex = -1;
        for (int i = 0; i < U.length; i++) {
            if (i == targetIndex) continue;
            double sim = cosineSimilarity(target, U[i]);
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Convert cosine similarity [-1,1] to compatibility percentage [0,100]
     */
    public double toCompatibilityPercent(double cosineSimilarity) {
        return Math.round(((cosineSimilarity + 1) / 2) * 100);
    }
}
