import os
import subprocess
import sys

# Try to import reportlab, install if not found
try:
    from reportlab.lib.pagesizes import letter
    from reportlab.lib import colors
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, HRFlowable
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
except ImportError:
    print("reportlab not found. Installing reportlab...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "reportlab"])
    from reportlab.lib.pagesizes import letter
    from reportlab.lib import colors
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, HRFlowable
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle

def generate_pdf():
    pdf_filename = "Amazon_MLSS_2026_SOP.pdf"
    
    # Page setup - 0.75 in (54 pt) margins
    doc = SimpleDocTemplate(
        pdf_filename,
        pagesize=letter,
        rightMargin=54,
        leftMargin=54,
        topMargin=54,
        bottomMargin=54
    )
    
    story = []
    styles = getSampleStyleSheet()
    
    # Colors
    primary_color = colors.HexColor("#1e293b")  # Dark slate
    accent_color = colors.HexColor("#2563eb")   # Royal blue
    text_color = colors.HexColor("#334155")     # Muted slate
    
    # Custom Styles
    title_style = ParagraphStyle(
        'HeaderTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=18,
        leading=22,
        textColor=primary_color,
        alignment=0, # Left
        spaceAfter=4
    )
    
    info_style = ParagraphStyle(
        'HeaderInfo',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=12,
        textColor=text_color,
        alignment=2 # Right
    )
    
    body_style = ParagraphStyle(
        'BodyTextCustom',
        parent=styles['BodyText'],
        fontName='Helvetica',
        fontSize=10.5,
        leading=15.5,
        textColor=text_color,
        spaceAfter=11
    )
    
    footer_style = ParagraphStyle(
        'WordCountFooter',
        parent=styles['Italic'],
        fontName='Helvetica-Oblique',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor("#64748b"),
        alignment=1, # Center
        spaceBefore=15
    )

    # 1. Header Section
    header_text = "<b>Statement of Purpose</b><br/>Amazon Machine Learning Summer School 2026"
    story.append(Paragraph(header_text, title_style))
    
    info_text = "<b>Applicant:</b> Piyush Bagdi<br/><b>Email:</b> bagdipiyush91@gmail.com"
    story.append(Paragraph(info_text, info_style))
    story.append(Spacer(1, 10))
    
    # Horizontal rule
    story.append(HRFlowable(width="100%", thickness=1.5, color=primary_color, spaceBefore=5, spaceAfter=15))
    
    # 2. Document Content
    paragraphs = [
        "I’ve always loved building software that solves real, practical problems. A while back, I built a desktop-based <b>Online Examination System</b> using Java, JavaFX, and PostgreSQL. It had all the standard features—role-based access, automated grading, and a session-based exam timer. But as I watched it in action, I realized a major flaw: teachers and admins were spending hours manually writing and formatting questions to align with syllabus standards.",
        
        "To automate this, I started looking into AI and NLP, which led me to build <b>GATE MockAI</b> (GitHub: github.com/piyush118-b/GateMockAI). This is a web app (React/Spring Boot) that generates realistic mock tests for the GATE exam using a <b>Semantic RAG Ingestion Pipeline</b>. One of the toughest parts was processing the PDF past papers. Standard text chunking kept cutting off multi-page math and coding questions. To fix this, I built a custom <b>sliding 3-page overlapping chunking window</b> so the context around every question stayed intact.",
        
        "I set up a local Ollama instance running <b>nomic-embed-text</b> for search and <b>qwen2.5-coder:7b</b> for extraction. I quickly learned that asking the LLM to do everything at once led to errors and timeouts. Instead, I decoupled the workflow: I used the LLM strictly to transcribe the PDF questions into raw JSON. Then, I wrote a Java regex parser to clean the output, cross-reference it with the official answer keys, calculate grading parameters, and set tolerances for Numerical Answer Type (NAT) questions. For generating tests, I used PostgreSQL's <b>pgvector</b> to run similarity searches, pulling real historical questions and feeding them as few-shot context to the generator so the outputs weren't generic or simplified.",
        
        "Working on GATE MockAI made me realize how much more I need to learn. While running local models is great for development, I want to learn how to <b>fine-tune and quantize</b> smaller LLMs so they can handle complex mathematical reasoning without needing expensive server hardware. I also want to understand <b>vector indexing optimizations</b> (like HNSW vs. IVFFlat) so search queries remain fast as the database grows, and learn how to move from local setups to <b>distributed MLOps pipelines</b> in production.",
        
        "This is exactly why I want to attend the Amazon ML Summer School. The curriculum on deep learning, generative AI, and scalable system design addresses my exact learning goals. I’m a hands-on developer who enjoys diving deep into data parsing, retrieval pipelines, and backend architecture. I’d love the chance to learn from Amazon's ML scientists and collaborate with other developers. MLSS will help me transition from someone who builds applications with AI into a specialized machine learning engineer."
    ]
    
    for para in paragraphs:
        story.append(Paragraph(para, body_style))
        
    # Word Count
    word_count = sum(len(p.split()) for p in paragraphs)
    story.append(Spacer(1, 10))
    story.append(HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#cbd5e1"), spaceBefore=10, spaceAfter=10))
    story.append(Paragraph(f"Word Count: {word_count} words (Limit: 500 words)", footer_style))
    
    doc.build(story)
    print(f"PDF successfully generated: {pdf_filename}")

if __name__ == "__main__":
    generate_pdf()
