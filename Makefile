pr_libs=java-advanced-2016/lib
pr_artifacts=java-advanced-2016/artifacts
compiled=out/production/java2016
pr_mypkg=ru.ifmo.ctddev.gafarov
pr_kgpkg=info.kgeorgiy.java.advanced
pr_pppkg=ru.ifmo.ctddev.poperechnyi

CPDEFAULT="-cp ${pr_libs}/*:${compiled}:${pr_artifacts}/"

build:
	find src/ -name *.java | xargs javac -d out/production/java2016/ -cp "java-advanced-2016/artifacts/*:java-advanced-2016/lib/*"

pull:
	git -C java-advanced-2016/ pull

hw1:
	java "${CPDEFAULT}/WalkTest.jar" ${pr_kgpkg}.walk.Tester RecursiveWalk ${pr_mypkg}.walk.RecursiveWalker

hw2:
	java "${CPDEFAULT}/ArraySetTest.jar" ${pr_kgpkg}.arrayset.Tester NavigableSet ${pr_mypkg}.arrayset.ArraySet

hw3:
	java "${CPDEFAULT}/ImplementorTest.jar" ${pr_kgpkg}.implementor.Tester class ${pr_mypkg}.implementor.Implementor

hw4:

hw5:
	java "${CPDEFAULT}/ImplementorTest.jar" ${pr_kgpkg}.implementor.Tester jar-class ${pr_mypkg}.implementor.Implementor

hw6:
	java "${CPDEFAULT}/IterativeParallelismTest.jar" ${pr_kgpkg}.concurrent.Tester list ${pr_mypkg}.concurrent.IterativeParallelism
hw7:
	java "${CPDEFAULT}/ParallelMapperTest.jar" ${pr_kgpkg}.mapper.Tester list ${pr_mypkg}.mapper.ParallelMapperImpl,${pr_mypkg}.mapper.IterativeParallelism
hw71:
	java "${CPDEFAULT}/ParallelMapperTest.jar" ${pr_kgpkg}.mapper.Tester list ${pr_pppkg}.parallelmapper.ParallelMapperImpl,${pr_pppkg}.parallelmapper.IterativeParallelism


hw8:
	java "${CPDEFAULT}/WebCrawlerTest.jar" ${pr_kgpkg}.crawler.Tester easy ${pr_mypkg}.crawler.WebCrawler

javadoc:
	rm -rf doc
	mkdir doc
	javadoc -d doc/ -sourcepath "src/:java-advanced-2016/java" -link https://docs.oracle.com/javase/8/docs/api/ \
		-private ru.ifmo.ctddev.gafarov.implementor java-advanced-2016/java/info/kgeorgiy/java/advanced/implementor/*.java

jar:
	jar cvfm Implementor.jar MANIFEST.MF -C out/production/java2016 ru/ifmo/ctddev/gafarov/implementor/Implementor.class 'ru/ifmo/ctddev/gafarov/implementor/Implementor.ClassDescriber.class'

