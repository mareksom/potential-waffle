CODE_BOLD       = (1, 21)
CODE_DIM        = (2, 22)
CODE_UNDERLINED = (4, 24)
CODE_BLINK      = (5, 25)
CODE_REVERSE    = (7, 27)
CODE_HIDDEN     = (8, 28)

DECORATIONS = ('BOLD', 'DIM', 'UNDERLINED', 'BLINK', 'REVERSE', 'HIDDEN')

CODE_DEFAULT       = 39
CODE_BLACK         = 30
CODE_RED           = 31
CODE_GREEN         = 32
CODE_YELLOW        = 33
CODE_BLUE          = 34
CODE_MAGENTA       = 35
CODE_CYAN          = 36
CODE_LIGHT_GRAY    = 37
CODE_DARK_GRAY     = 90
CODE_LIGHT_GREEN   = 92
CODE_LIGHT_YELLOW  = 93
CODE_LIGHT_BLUE    = 94
CODE_LIGHT_MAGENTA = 95
CODE_LIGHT_CYAN    = 96
CODE_WHITE         = 97

COLORS = ('DEFAULT', 'BLACK', 'RED', 'GREEN', 'YELLOW', 'BLUE', 'MAGENTA', 'CYAN', 'LIGHT_GRAY', 'DARK_GRAY', 'LIGHT_GREEN', 'LIGHT_YELLOW', 'LIGHT_BLUE', 'LIGHT_MAGENTA', 'LIGHT_CYAN', 'WHITE')

CODE_BACKGROUND_DEFAULT       = 49
CODE_BACKGROUND_BLACK         = 40
CODE_BACKGROUND_RED           = 41
CODE_BACKGROUND_GREEN         = 42
CODE_BACKGROUND_YELLOW        = 43
CODE_BACKGROUND_BLUE          = 44
CODE_BACKGROUND_MAGENTA       = 45
CODE_BACKGROUND_CYAN          = 46
CODE_BACKGROUND_LIGHT_GRAY    = 47
CODE_BACKGROUND_DARK_GRAY     = 100
CODE_BACKGROUND_LIGHT_GREEN   = 102
CODE_BACKGROUND_LIGHT_YELLOW  = 103
CODE_BACKGROUND_LIGHT_BLUE    = 104
CODE_BACKGROUND_LIGHT_MAGENTA = 105
CODE_BACKGROUND_LIGHT_CYAN    = 106
CODE_BACKGROUND_WHITE         = 107

def code(num):
	return '"\033[{}m"'.format(num)

for d in DECORATIONS:
	a, b = globals()['CODE_' + d]
	exec("""def {}(message):
	return {} + message + {}
	""".format(d, code(a), code(b)))

for c in COLORS:
	a = globals()['CODE_' + c]
	b = globals()['CODE_' + 'DEFAULT']
	exec("""def {}(message):
	return {} + message + {}
	""".format(c, code(a), code(b)))

	a = globals()['CODE_BACKGROUND_' + c]
	b = globals()['CODE_BACKGROUND_' + 'DEFAULT']
	exec("""def BACKGROUND_{}(message):
	return {} + message + {}
	""".format(c, code(a), code(b)))


# These are tests, designed to see how every decoration/color looks like in your terminal
#
#print(BOLD('A quick brown fox jumped over the lazy dog.'), ' --- BOLD')
#print(DIM('A quick brown fox jumped over the lazy dog.'), ' --- DIM')
#print(UNDERLINED('A quick brown fox jumped over the lazy dog.'), ' --- UNDERLINED')
#print(BLINK('A quick brown fox jumped over the lazy dog.'), ' --- BLINK')
#print(REVERSE('A quick brown fox jumped over the lazy dog.'), ' --- REVERSE')
#print(HIDDEN('A quick brown fox jumped over the lazy dog.'), ' --- HIDDEN')
#print(DEFAULT('A quick brown fox jumped over the lazy dog.'), ' --- DEFAULT')
#print(BLACK('A quick brown fox jumped over the lazy dog.'), ' --- BLACK')
#print(RED('A quick brown fox jumped over the lazy dog.'), ' --- RED')
#print(GREEN('A quick brown fox jumped over the lazy dog.'), ' --- GREEN')
#print(YELLOW('A quick brown fox jumped over the lazy dog.'), ' --- YELLOW')
#print(BLUE('A quick brown fox jumped over the lazy dog.'), ' --- BLUE')
#print(MAGENTA('A quick brown fox jumped over the lazy dog.'), ' --- MAGENTA')
#print(CYAN('A quick brown fox jumped over the lazy dog.'), ' --- CYAN')
#print(LIGHT_GRAY('A quick brown fox jumped over the lazy dog.'), ' --- LIGHT_GRAY')
#print(DARK_GRAY('A quick brown fox jumped over the lazy dog.'), ' --- DARK_GRAY')
#print(LIGHT_GREEN('A quick brown fox jumped over the lazy dog.'), ' --- LIGHT_GREEN')
#print(LIGHT_YELLOW('A quick brown fox jumped over the lazy dog.'), ' --- LIGHT_YELLOW')
#print(LIGHT_BLUE('A quick brown fox jumped over the lazy dog.'), ' --- LIGHT_BLUE')
#print(LIGHT_MAGENTA('A quick brown fox jumped over the lazy dog.'), ' --- LIGHT_MAGENTA')
#print(LIGHT_CYAN('A quick brown fox jumped over the lazy dog.'), ' --- LIGHT_CYAN')
#print(WHITE('A quick brown fox jumped over the lazy dog.'), ' --- WHITE')
#print(BACKGROUND_DEFAULT('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_DEFAULT')
#print(BACKGROUND_BLACK('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_BLACK')
#print(BACKGROUND_RED('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_RED')
#print(BACKGROUND_GREEN('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_GREEN')
#print(BACKGROUND_YELLOW('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_YELLOW')
#print(BACKGROUND_BLUE('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_BLUE')
#print(BACKGROUND_MAGENTA('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_MAGENTA')
#print(BACKGROUND_CYAN('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_CYAN')
#print(BACKGROUND_LIGHT_GRAY('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_LIGHT_GRAY')
#print(BACKGROUND_DARK_GRAY('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_DARK_GRAY')
#print(BACKGROUND_LIGHT_GREEN('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_LIGHT_GREEN')
#print(BACKGROUND_LIGHT_YELLOW('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_LIGHT_YELLOW')
#print(BACKGROUND_LIGHT_BLUE('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_LIGHT_BLUE')
#print(BACKGROUND_LIGHT_MAGENTA('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_LIGHT_MAGENTA')
#print(BACKGROUND_LIGHT_CYAN('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_LIGHT_CYAN')
#print(BACKGROUND_WHITE('A quick brown fox jumped over the lazy dog.'), ' --- BACKGROUND_WHITE')
