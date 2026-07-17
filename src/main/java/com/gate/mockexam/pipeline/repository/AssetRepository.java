package com.gate.mockexam.pipeline.repository;

import com.gate.mockexam.pipeline.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {

    List<Asset> findByQuestionQuestionId(String questionId);

    List<Asset> findByQuestionQuestionIdAndAssetType(String questionId, String assetType);
}
