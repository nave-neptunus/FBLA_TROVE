import re

def fix_css(filename):
    with open(filename, 'r') as f:
        content = f.read()

    props = ['font-family', 'font-size', 'background-color', 'border-color', 'border-width', 'border-radius', 'padding', 'cursor', 'font-weight', 'background']
    
    lines = content.split('\n')
    new_lines = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        new_lines.append(line)
        
        for prop in props:
            pattern = r'-fx-' + prop + r'\s*:\s*(.*?)(;|$)'
            match = re.search(pattern, line)
            if match:
                indent_match = re.match(r'(\s*)', line)
                indent = indent_match.group(1) if indent_match else ''
                value = match.group(1).strip()
                std_prop_line = f"{indent}{prop}: {value};"
                
                # check if next line is already the standard property
                if i + 1 < len(lines) and lines[i+1].strip().startswith(f"{prop}:"):
                    pass # it's already there
                else:
                    new_lines.append(std_prop_line)
                break
        i += 1
                
    with open(filename, 'w') as f:
        f.write('\n'.join(new_lines))

fix_css('src/main/resources/styles.css')
fix_css('src/main/resources/styles-dark.css')
print("Done fixing CSS")
