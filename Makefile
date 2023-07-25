# Define the Java compiler and flags
JAVAC = javac
JFLAGS = -d .

# Define the source directory
SRCDIR = src

# Define the classpath
CLASSPATH = -cp .

# Define the main class
MAIN_CLASS = App

# Define the default target (what will be built when you run 'make' without any arguments)
all: build

# Target for compiling Java source files
build: $(SOURCES)
	$(JAVAC) $(JFLAGS) $(SOURCES)

# Target for moving class files to the root directory
move: build
	@find . -name "*.class" -print0 | xargs -0 mv -t .

# Target for running the Java program
run:
	java $(CLASSPATH) $(MAIN_CLASS)

# Target for cleaning (removing generated class files)
clean:
	rm -f *.class

# Phony targets to avoid conflicts with files/folders named "clean", "run", and "move"
.PHONY: all build run clean move

# Collect all Java source files recursively using wildcard function
SOURCES := $(wildcard $(SRCDIR)/**/*.java $(SRCDIR)/*.java)
