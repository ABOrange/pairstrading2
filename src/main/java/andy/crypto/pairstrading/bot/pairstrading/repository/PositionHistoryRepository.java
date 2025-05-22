package andy.crypto.pairstrading.bot.pairstrading.repository;

import andy.crypto.pairstrading.bot.pairstrading.model.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {
    
    /**
     * 按照交易對查詢並按時間戳降序排序
     */
    List<PositionHistory> findBySymbolOrderByTimestampDesc(String symbol);
    
    /**
     * 查詢所有記錄並按時間戳降序排序
     */
    List<PositionHistory> findAllByOrderByTimestampDesc();
    
    /**
     * 查詢最近的N條記錄
     */
    @Query(value = "SELECT * FROM position_history ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<PositionHistory> findRecentPositionHistory(@Param("limit") int limit);
    
    /**
     * 查詢指定交易對的最近N條記錄
     */
    @Query(value = "SELECT * FROM position_history WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<PositionHistory> findRecentBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);
}
