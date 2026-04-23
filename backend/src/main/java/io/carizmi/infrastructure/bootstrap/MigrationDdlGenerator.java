package io.carizmi.infrastructure.bootstrap;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Table;
import liquibase.structure.core.Column;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.UniqueConstraint;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MigrationDdlGenerator {
    private static final Logger log = LoggerFactory.getLogger(MigrationDdlGenerator.class);
    private static final String GENERATED_CHANGELOG_XML = "generated-changelog.xml";

    public static void main(String[] args) {
        System.setProperty("liquibase.suppressLiquibaseSql", "true");
//        System.setProperty("spring.main.web-application-type", "none");

        String profile = System.getProperty("migration.profile", "liquibase-diff");
        log.info("Starting MigrationDdlGenerator with profile: {}", profile);

        Path resourcesPath = getResourcesPath();
        File migrationDir = resourcesPath.resolve("db").resolve("migration").toFile();
        try {
            Files.createDirectories(migrationDir.toPath());
        } catch (IOException e) {
            log.error("migration dir creation failed: ", e);
            throw new RuntimeException(e);
        }

        File changelogXmlOutFile = new File(migrationDir, GENERATED_CHANGELOG_XML);
        String nextFileName = getNextMigrationFileName();
        File sqlOutFile = new File(migrationDir, nextFileName);
        log.info("Generating next migration file: {}", nextFileName);

        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .web(WebApplicationType.NONE)
                .profiles(profile)
                .sources(JpaLiquibaseConfig.class)
                .run(args);
        DataSource dataSource = context.getBean(DataSource.class);

        boolean changelogCreated = generatedChangelogXml(dataSource, changelogXmlOutFile);

        if (changelogCreated) {
            generateDDL(dataSource, sqlOutFile, resourcesPath);
        }
    }

    private static boolean generatedChangelogXml(DataSource dataSource, File changelogXmlOut) {
        try (Connection conn = dataSource.getConnection()) {
            Database targetDatabase = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            // Reference metadata from Hibernate/JPA
            String entityPackage = System.getProperty("migration.entity.package", "io.carizmi.domain");
            String hibernateDialect = System.getProperty("migration.hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            String referenceUrl = "hibernate:spring:" + entityPackage + "?dialect=" + hibernateDialect;

            Database referenceDatabase = DatabaseFactory.getInstance()
                    .openDatabase(referenceUrl, null, null, null, null);

            if (referenceDatabase == null) {
                throw new IllegalStateException("Reference database (Hibernate metadata) could not be opened. " +
                        "Check liquibase-hibernate on the classpath and the referenceUrl.");
            }
            if (targetDatabase == null) {
                throw new IllegalStateException("Target database could not be opened. Check JDBC URL and connectivity.");
            }

            // produce diff
            DiffResult diffResult;
            try {
                CompareControl compareControl = new CompareControl();
                diffResult = DiffGeneratorFactory.getInstance()
                        .compare(referenceDatabase, targetDatabase, compareControl);

                // Deep Filter: Filter out non-domain tables AND all their associated objects (Columns, PKs, etc.)
                filterNonDomainObjects(diffResult.getUnexpectedObjects());
                filterNonDomainObjects(diffResult.getMissingObjects());
            } catch (LiquibaseException e) {
                log.error("Liquibase diff failed:", e);
                throw e;
            }

            // write changelog into XML
            try (PrintStream ps = new PrintStream(new FileOutputStream(changelogXmlOut))) {
                DiffToChangeLog changeLogWriter = new DiffToChangeLog(diffResult, new DiffOutputControl(false, false, false, null));
                changeLogWriter.print(ps);
            }

            log.info("Generated changelog XML: {}", changelogXmlOut.getAbsolutePath());
            return changelogXmlOut.exists();
        } catch (SQLException | LiquibaseException | ParserConfigurationException | IOException e) {
            log.error("Changelog XML generation failed:", e);
            throw new RuntimeException(e);
        }
    }

    private static void filterNonDomainObjects(Collection<? extends DatabaseObject> objects) {
        objects.removeIf(MigrationDdlGenerator::isExcludedObject);
    }

    private static boolean isExcludedObject(DatabaseObject obj) {
        String tableName = null;

        if (obj instanceof Table) {
            tableName = obj.getName();
        } else if (obj instanceof Column) {
            tableName = ((Column) obj).getRelation().getName();
        } else if (obj instanceof Index) {
            tableName = ((Index) obj).getRelation().getName();
        } else if (obj instanceof PrimaryKey) {
            tableName = ((PrimaryKey) obj).getTable().getName();
        } else if (obj instanceof ForeignKey) {
            tableName = ((ForeignKey) obj).getForeignKeyTable().getName();
        } else if (obj instanceof UniqueConstraint) {
            tableName = ((UniqueConstraint) obj).getRelation().getName();
        }

        if (tableName != null) {
            String lowerTable = tableName.toLowerCase();
            return lowerTable.startsWith("flyway_") 
                    || lowerTable.equals("databasechangelog") 
                    || lowerTable.equals("databasechangeloglock");
        }
        return false;
    }

    private static void generateDDL(DataSource dataSource, File sqlOutFile, Path resourcesPath) {
        String changelogFile = "db/migration/" + GENERATED_CHANGELOG_XML;

        try (Connection conn = dataSource.getConnection()) {
            Database targetDatabase = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));

            // convert changelog XML to SQL and write to .sql file
            try (ResourceAccessor resourceAccessor = new DirectoryResourceAccessor(resourcesPath);
                 Liquibase liquibase = new Liquibase(changelogFile, resourceAccessor, targetDatabase);
                 Writer writer = new FileWriter(sqlOutFile.getAbsolutePath())) {
                liquibase.updateSql(new Contexts(), new LabelExpression(), writer);
                System.out.println("SQL generated successfully to: " + sqlOutFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (SQLException | DatabaseException e) {
            throw new RuntimeException(e);
        }

        try {
            List<String> lines = Files.readAllLines(sqlOutFile.toPath());
            List<String> filtered = lines.stream()
                    .filter(l -> !l.matches("^--\\s*(Change Log:|Ran at:|Against:|Liquibase version:|\\*{3,}).*"))
                    .filter(l -> !l.matches("^--\\s*Changeset .*\\(generated\\).*"))
                    .collect(Collectors.toList());
            Files.write(sqlOutFile.toPath(), filtered);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getNextMigrationFileName() {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("V%s__DDL.sql", timestamp);
    }

    private static Path getResourcesPath() {
        String resourcesDirProp = System.getProperty("migration.resources.dir");
        Path resourcesPath;
        if (resourcesDirProp != null && !resourcesDirProp.isBlank()) {
            resourcesPath = Paths.get(resourcesDirProp).toAbsolutePath().normalize();
        } else {
            // default: try to resolve backend module relative to current working dir
            Path cwd = Paths.get("").toAbsolutePath().normalize();
            Path backendCandidate = cwd.resolve("backend").resolve("src").resolve("main").resolve("resources");
            if (Files.exists(backendCandidate.getParent())) {
                resourcesPath = backendCandidate;
            } else {
                // fallback to plain src/main/resources under cwd
                resourcesPath = cwd.resolve("src").resolve("main").resolve("resources");
            }
        }
        return resourcesPath;
    }
}