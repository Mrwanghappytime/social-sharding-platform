package com.social.relation.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.single.config.SingleRuleConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Configuration
public class ShardingSphereDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() throws SQLException {
        try {
            Map<String, DataSource> dataSourceMap = new HashMap<>();
            dataSourceMap.put("ds0", createPhysicalDataSource());

            ShardingRuleConfiguration shardingRuleConfiguration = createShardingRuleConfiguration();
            SingleRuleConfiguration singleRuleConfiguration = new SingleRuleConfiguration(Collections.singleton("*.*"), "ds0");
            ArrayList<RuleConfiguration> ruleConfigurations = new ArrayList<>();
            ruleConfigurations.add(shardingRuleConfiguration);
            ruleConfigurations.add(singleRuleConfiguration);
            Properties props = new Properties();
            props.setProperty("sql-show", "true");

            return ShardingSphereDataSourceFactory.createDataSource(dataSourceMap, ruleConfigurations, props);
        } catch (Throwable e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            throw new IllegalStateException(
                    "Failed to create ShardingSphere DataSource: "
                            + e.getClass().getName() + " - " + e.getMessage()
                            + "; rootCause=" + root.getClass().getName() + " - " + root.getMessage(),
                    e
            );
        }
    }

    private DataSource createPhysicalDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private ShardingRuleConfiguration createShardingRuleConfiguration() {
        ShardingRuleConfiguration result = new ShardingRuleConfiguration();
        result.getTables().add(createFollowingTableRule());
        result.getTables().add(createFollowersTableRule());
        result.getShardingAlgorithms().put("following-inline", createInlineAlgorithm("following_$->{follower_id % 16}"));
        result.getShardingAlgorithms().put("followers-inline", createInlineAlgorithm("followers_$->{following_id % 16}"));
        return result;
    }

    private ShardingTableRuleConfiguration createFollowingTableRule() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration(
                "following",
                "ds0.following_$->{0..15}"
        );
        result.setTableShardingStrategy(new StandardShardingStrategyConfiguration("follower_id", "following-inline"));
        return result;
    }

    private ShardingTableRuleConfiguration createFollowersTableRule() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration(
                "followers",
                "ds0.followers_$->{0..15}"
        );
        result.setTableShardingStrategy(new StandardShardingStrategyConfiguration("following_id", "followers-inline"));
        return result;
    }

    private AlgorithmConfiguration createInlineAlgorithm(String expression) {
        Properties props = new Properties();
        props.setProperty("algorithm-expression", expression);
        return new AlgorithmConfiguration("INLINE", props);
    }
}
