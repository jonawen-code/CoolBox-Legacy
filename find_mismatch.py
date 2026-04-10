
import sys

def find_mismatch(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    stack = []
    for i, line in enumerate(lines):
        line_num = i + 1
        for char in line:
            if char == '{':
                stack.append(line_num)
            elif char == '}':
                if not stack:
                    print(f"Extra closing brace at line {line_num}")
                else:
                    stack.pop()
    
    if stack:
        print(f"Unclosed braces opened at lines: {stack}")
    else:
        print("No mismatch found.")

if __name__ == "__main__":
    find_mismatch(sys.argv[1])
