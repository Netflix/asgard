/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
ruleset {

    description 'Asgard RuleSet containing all CodeNarc Rules, grouped by category.'

//    rule('file:test/unit/com/netflix/asgard/codenarc/ExplicitCallToSpringAutoWire.groovy') {
//        description = 'Use of Spring.autowire(...) can become unstable.'
//    }

    // rulesets/basic.xml
    AssertWithinFinallyBlock
    AssignmentInConditional
    BigDecimalInstantiation
    BitwiseOperatorInConditional
    BooleanGetBoolean {
        priority = 1
    }
    BrokenNullCheck
    BrokenOddnessCheck
    ClassForName
    ComparisonOfTwoConstants
    ComparisonWithSelf
    ConstantAssertExpression
    ConstantIfExpression
    ConstantTernaryExpression
    DeadCode
    DoubleNegative
    DuplicateCaseStatement
    DuplicateMapKey
    DuplicateSetValue
    EmptyCatchBlock
    EmptyElseBlock
    EmptyFinallyBlock
    EmptyForStatement
    EmptyIfStatement
    EmptyInstanceInitializer
//    EmptyMethod
    EmptyStaticInitializer
    EmptySwitchStatement
    EmptySynchronizedStatement
    EmptyTryBlock
    EmptyWhileStatement
    EqualsAndHashCode
    EqualsOverloaded
    ExplicitGarbageCollection
    ForLoopShouldBeWhileLoop
    HardCodedWindowsFileSeparator
    HardCodedWindowsRootDirectory
    IntegerGetInteger
    RandomDoubleCoercedToZero
    RemoveAllOnSelf
    ReturnFromFinallyBlock
    ThrowExceptionFromFinallyBlock

    // rulesets/braces.xml
    ElseBlockBraces
    ForStatementBraces
    IfStatementBraces
    WhileStatementBraces

    // rulesets/concurrency.xml
    BusyWait
    DoubleCheckedLocking
    InconsistentPropertyLocking
    InconsistentPropertySynchronization
    NestedSynchronization
    StaticCalendarField
    StaticConnection
    StaticDateFormatField
    StaticMatcherField
    StaticSimpleDateFormatField
    SynchronizedMethod
    SynchronizedOnBoxedPrimitive
    SynchronizedOnGetClass
    SynchronizedOnReentrantLock
    SynchronizedOnString
    SynchronizedOnThis
    SynchronizedReadObjectMethod
    SystemRunFinalizersOnExit
//    ThisReferenceEscapesConstructor
    ThreadGroup
    ThreadLocalNotStaticFinal
    ThreadYield
    UseOfNotifyMethod
    VolatileArrayField
    VolatileLongOrDoubleField
    WaitOutsideOfWhileLoop

    // rulesets/convention.xml
    ConfusingTernary
//    CouldBeElvis
    HashtableIsObsolete
    IfStatementCouldBeTernary
//    InvertedIfElse
    LongLiteralWithLowerCaseL
    ParameterReassignment
    TernaryCouldBeElvis
    VectorIsObsolete

    // rulesets/design.xml
//    AbstractClassWithPublicConstructor  // nothing wrong with this
//    AbstractClassWithoutAbstractMethod  // nothing wrong with this
    BooleanMethodReturnsNull
//    BuilderMethodWithSideEffects
    CloneableWithoutClone
    CloseWithoutCloseable
    CompareToWithoutComparable
    ConstantsOnlyInterface
//    EmptyMethodInAbstractClass  // nothing wrong with this
    FinalClassWithProtectedMember
    ImplementationAsType
//    PrivateFieldCouldBeFinal
    PublicInstanceField
    ReturnsNullInsteadOfEmptyArray
    ReturnsNullInsteadOfEmptyCollection
    SimpleDateFormatMissingLocale
    StatelessSingleton

    // rulesets/dry.xml
//    DuplicateListLiteral
//    DuplicateMapLiteral
//    DuplicateNumberLiteral
//    DuplicateStringLiteral

    // rulesets/exceptions.xml
    CatchArrayIndexOutOfBoundsException
    CatchError
//    CatchException
    CatchIllegalMonitorStateException
    CatchIndexOutOfBoundsException
//    CatchNullPointerException
    CatchRuntimeException
    CatchThrowable
    ConfusingClassNamedException
    ExceptionExtendsError
    ExceptionNotThrown
    MissingNewInThrowStatement
//    ReturnNullFromCatchBlock
    SwallowThreadDeath
    ThrowError
    ThrowException
//    ThrowNullPointerException
    ThrowRuntimeException
    ThrowThrowable

    // rulesets/formatting.xml
    BracesForClass
    BracesForForLoop
    BracesForIfElse
    BracesForMethod
    BracesForTryCatchFinally
    ClassJavadoc
//    LineLength
    SpaceAfterCatch
//    SpaceAfterClosingBrace
    SpaceAfterComma
    SpaceAfterFor
    SpaceAfterIf
    SpaceAfterOpeningBrace
    SpaceAfterSemicolon
    SpaceAfterSwitch
    SpaceAfterWhile
    SpaceAroundClosureArrow
    SpaceAroundOperator
    SpaceBeforeClosingBrace
//    SpaceBeforeOpeningBrace

    // rulesets/generic.xml
    IllegalClassMember
    IllegalClassReference
    IllegalPackageReference
    IllegalRegex
    RequiredRegex
    RequiredString
    StatelessClass

    // rulesets/grails.xml
//    GrailsDomainHasEquals
//    GrailsDomainHasToString
    GrailsDuplicateMapping
//    GrailsPublicControllerMethod
    GrailsServletContextReference
    GrailsSessionReference
//    GrailsStatelessService
    GrailsDuplicateConstraint
    GrailsDomainWithServiceReference
    GrailsDomainReservedSqlKeywordName

    // rulesets/groovyism.xml
//    AssignCollectionSort
    AssignCollectionUnique
//    ClosureAsLastMethodParameter
    CollectAllIsDeprecated
    ConfusingMultipleReturns
    ExplicitArrayListInstantiation
    ExplicitCallToAndMethod
    ExplicitCallToCompareToMethod
    ExplicitCallToDivMethod
    ExplicitCallToEqualsMethod
    ExplicitCallToGetAtMethod
    ExplicitCallToLeftShiftMethod
    ExplicitCallToMinusMethod
    ExplicitCallToModMethod
    ExplicitCallToMultiplyMethod
    ExplicitCallToOrMethod
    ExplicitCallToPlusMethod
    ExplicitCallToPowerMethod
    ExplicitCallToRightShiftMethod
    ExplicitCallToXorMethod
    ExplicitHashMapInstantiation
    ExplicitHashSetInstantiation
    ExplicitLinkedHashMapInstantiation
    ExplicitLinkedListInstantiation
    ExplicitStackInstantiation
//    ExplicitTreeSetInstantiation
    GStringAsMapKey
//    GetterMethodCouldBeProperty
    GroovyLangImmutable
//    GStringExpressionWithinString
//    UseCollectMany
    UseCollectNested

    // rulesets/imports.xml
    DuplicateImport
    ImportFromSamePackage
    ImportFromSunPackages
    MisorderedStaticImports
    UnnecessaryGroovyImport
    UnusedImport

    // rulesets/jdbc.xml
    DirectConnectionManagement
    JdbcConnectionReference
    JdbcResultSetReference
    JdbcStatementReference

    // rulesets/junit.xml
    ChainedTest
    CoupledTestCase
    JUnitAssertAlwaysFails
    JUnitAssertAlwaysSucceeds
    JUnitFailWithoutMessage
//    JUnitLostTest
    JUnitPublicField
    JUnitPublicNonTestMethod
//    JUnitSetUpCallsSuper
//    JUnitStyleAssertions
//    JUnitTearDownCallsSuper
    JUnitTestMethodWithoutAssert
    JUnitUnnecessarySetUp
    JUnitUnnecessaryThrowsException
    JUnitUnnecessaryTearDown
    SpockIgnoreRestUsed
    UnnecessaryFail
    UseAssertEqualsInsteadOfAssertTrue
    UseAssertFalseInsteadOfNegation
    UseAssertNullInsteadOfAssertEquals
    UseAssertSameInsteadOfAssertTrue
    UseAssertTrueInsteadOfAssertEquals
    UseAssertTrueInsteadOfNegation

    // rulesets/logging.xml
    LoggerForDifferentClass
    LoggerWithWrongModifiers
    LoggingSwallowsStacktrace
    MultipleLoggers
    PrintStackTrace {
        priority = 1
    }
    Println {
        priority = 1
    }
    SystemErrPrint {
        priority = 1
    }
    SystemOutPrint {
        priority = 1
    }

    // rulesets/naming.xml
    AbstractClassName
    ClassName
    ClassNameSameAsFilename
//    ConfusingMethodName
//    FactoryMethodName
//    FieldName // we are a bit lax about conventions here
    InterfaceName
//    MethodName
    ObjectOverrideMisspelledMethodName
    PackageName
    ParameterName
//    PropertyName
//    VariableName

    // rulesets/security.xml
    FileCreateTempFile
    InsecureRandom
//    JavaIoPackageAccess
    NonFinalPublicField
    NonFinalSubclassOfSensitiveInterface
    ObjectFinalize
    PublicFinalizeMethod
    SystemExit
    UnsafeArrayDeclaration

    // rulesets/serialization.xml
    EnumCustomSerializationIgnored
    SerialPersistentFields
    SerialVersionUID
//    SerializableClassMustDefineSerialVersionUID

    // rulesets/size.xml
//    AbcComplexity
//    AbcMetric
//    ClassSize
//    CrapMetric
    CyclomaticComplexity
//    MethodCount // the AWS classes we implement trigger this more than we do
//    MethodSize
//    NestedBlockDepth

    // rulesets/unnecessary.xml
    AddEmptyString
    ConsecutiveLiteralAppends
    ConsecutiveStringConcatenation
    UnnecessaryBigDecimalInstantiation
    UnnecessaryBigIntegerInstantiation
//    UnnecessaryBooleanExpression
    UnnecessaryBooleanInstantiation
    UnnecessaryCallForLastElement
    UnnecessaryCallToSubstring
    UnnecessaryCatchBlock
//    UnnecessaryCollectCall
    UnnecessaryCollectionCall
    UnnecessaryConstructor
    UnnecessaryDefInFieldDeclaration
    UnnecessaryDefInMethodDeclaration
    UnnecessaryDefInVariableDeclaration
    UnnecessaryDotClass
    UnnecessaryDoubleInstantiation
//    UnnecessaryElseStatement
    UnnecessaryFinalOnPrivateMethod
    UnnecessaryFloatInstantiation
//    UnnecessaryGString
//    UnnecessaryGetter
    UnnecessaryIfStatement
    UnnecessaryInstanceOfCheck
    UnnecessaryInstantiationToGetClass
    UnnecessaryIntegerInstantiation
    UnnecessaryLongInstantiation
    UnnecessaryModOne
    UnnecessaryNullCheck
    UnnecessaryNullCheckBeforeInstanceOf
//    UnnecessaryObjectReferences
    UnnecessaryOverridingMethod
    UnnecessaryPackageReference
//    UnnecessaryParenthesesForMethodCallWithClosure
//    UnnecessaryPublicModifier
//    UnnecessaryReturnKeyword
    UnnecessarySelfAssignment
    UnnecessarySemicolon
    UnnecessaryStringInstantiation
//    UnnecessarySubstring
    UnnecessaryTernaryExpression
    UnnecessaryTransientModifier

    // rulesets/unused.xml
    UnusedArray
//    UnusedMethodParameter
    UnusedObject
    UnusedPrivateField
    UnusedPrivateMethod
    UnusedPrivateMethodParameter
//    UnusedVariable

}
