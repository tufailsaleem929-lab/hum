package com.example.data.model

data class NoteTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val initialTitle: String,
    val defaultContent: String,
    val defaultTags: String,
    val defaultColor: String
)

val NOTE_TEMPLATES = listOf(
    NoteTemplate(
        id = "meeting",
        title = "Meeting Minutes",
        description = "Structured agenda, attendee discussion, and action items",
        icon = "groups",
        initialTitle = "Meeting: Sprint Sync",
        defaultContent = """
## 📅 Meeting Overview
- **Date & Time:** 
- **Attendees:** 
- **Goal:** 

### 💬 Discussion Points
1. 
2. 

### 🎯 Key Decisions
- 

### ⚡ Action Items
- [ ] Task 1 (Owner: )
- [ ] Task 2 (Owner: )
        """.trimIndent(),
        defaultTags = "Meeting, Work",
        defaultColor = "ocean"
    ),
    NoteTemplate(
        id = "daily_planner",
        title = "Daily Planner",
        description = "Priorities, time blocks, and end-of-day reflection",
        icon = "today",
        initialTitle = "Daily Plan: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
        defaultContent = """
### 🌟 Top 3 Priorities for Today
1. 
2. 
3. 

### ⏰ Schedule & Time Blocks
- **09:00 - 11:00:** Deep Work
- **11:30 - 12:30:** Team Sync & Review
- **14:00 - 16:00:** Execution & Deliverables

### 📝 Notes & Reflections
- What went well today?
- Key lessons learned:
        """.trimIndent(),
        defaultTags = "Planner, Daily",
        defaultColor = "amber"
    ),
    NoteTemplate(
        id = "project_brief",
        title = "Project Brief",
        description = "Problem statement, scope, timeline, and deliverables",
        icon = "rocket_launch",
        initialTitle = "Project Brief: New Initiative",
        defaultContent = """
### 🎯 Executive Summary
Brief summary of what this project aims to accomplish.

### ❓ Problem Statement
What critical user pain point does this solve?

### 📦 Key Deliverables
1. MVP Architecture & Interface
2. Testing & Quality Assurance
3. Staged Rollout

### 📈 Success Metrics
- 
        """.trimIndent(),
        defaultTags = "Project, Strategy",
        defaultColor = "violet"
    ),
    NoteTemplate(
        id = "brainstorm",
        title = "Brainstorming Canvas",
        description = "Unfiltered ideation, thematic clusters, and next steps",
        icon = "psychology",
        initialTitle = "Brainstorm: Creative Concepts",
        defaultContent = """
### 💡 Core Brainstorm Theme
What is the core question or challenge we are exploring?

### 🚀 Raw Ideas & Inspiration
- 
- 
- 

### ⭐ High-Potential Candidates
1. 
2. 

### 🔍 Next Experiments to Run
- 
        """.trimIndent(),
        defaultTags = "Ideas, Brainstorm",
        defaultColor = "sunset"
    ),
    NoteTemplate(
        id = "cornell",
        title = "Cornell Notes",
        description = "Cues column, detailed notes, and bottom summary",
        icon = "school",
        initialTitle = "Study Notes: Lecture & Reading",
        defaultContent = """
### 🔑 Cues & Questions
- Main Idea 1?
- Key Definition?

### 📖 Detailed Notes
Record lecture facts, formulas, and explanations here in bullet points.

### 📌 Summary (Bottom Section)
In 2-3 sentences, synthesize the big takeaway from these notes.
        """.trimIndent(),
        defaultTags = "Study, Cornell",
        defaultColor = "emerald"
    )
)
