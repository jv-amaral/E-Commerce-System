JAVAC   = javac
JAVA    = java
BIN_DIR = bin
JFLAGS  = -encoding UTF-8 -d $(BIN_DIR)

SOURCES = MarketPlace.java \
          $(wildcard model/*.java) \
          $(wildcard controller/*.java) \
          $(wildcard view/*.java)

MAIN_GUI = view.MarketPlaceSwing
MAIN_CLI = MarketPlace

.PHONY: all compile run gui cli clean help

all: compile

compile:
	@mkdir -p $(BIN_DIR)
	$(JAVAC) $(JFLAGS) $(SOURCES)

run: compile
	$(JAVA) -cp $(BIN_DIR) $(MAIN_GUI)

gui: run

cli: compile
	$(JAVA) -cp $(BIN_DIR) $(MAIN_CLI)

clean:
	rm -rf $(BIN_DIR)

help:
	@echo "Alvos disponíveis:"
	@echo "  make          - Compila todo o projeto em $(BIN_DIR)/"
	@echo "  make run      - Compila e executa a interface gráfica (Swing)"
	@echo "  make gui      - Atalho para 'make run'"
	@echo "  make cli      - Compila e executa a interface de terminal (CLI)"
	@echo "  make clean    - Remove os arquivos compilados ($(BIN_DIR)/)"
