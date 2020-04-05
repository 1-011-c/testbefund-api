package de.testbefund.testbefundapi.test.dto;

import de.testbefund.testbefundapi.test.data.TestCase;
import de.testbefund.testbefundapi.test.data.TestContainer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Maps the Read-Only part of the test cases to DTOs
 */
@Mapper
public interface TestReadMapper {

    TestReadMapper MAPPER = Mappers.getMapper(TestReadMapper.class);

    @Mapping(source = "readId", target = "uuid_read")
    @Mapping(source = "testCases", target = "tests")
    TestContainerReadT mapOne(TestContainer testContainer);

    @Mapping(source = "result", target = "infected")
    TestCaseReadT mapOne(TestCase testCase);
}