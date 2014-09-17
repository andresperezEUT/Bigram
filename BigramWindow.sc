// TODO: default subdivision menu to default subdivision (4)
// TODO: circle size independent from time subdivision

BigramWindow : QWindow {

	var <bigramView;
	var <vSlider, <hSlider, <zSlider;
	var insertButton, editButton;
	var buttonList;
	var beatSubdivisionMenu;

	var <elementSize = 30;
	var <bigramSpace = 50; //vertical
	var <bigramSubdivisionSpace; //= bigramSpace / 6;  //(2 bigramSpace in a octave)
	var <upperSpace = 50;
	var <leftSpace = 50;
	var <beatLineSpace = 40; //horizontal (time subdivision)
	var <beatSubdivision = 4;

	var <numOctaves = 5;

	var lastMouseDownPosition;
	var selectionRectangle;
	var noteSelectedInLastDown;

	var <mode;


	// internal representation
	// var <notes; //list

	var <bigram;

	*new { arg name = "Bigram Editor", bounds, resizable = true, border = true, server, scroll = false;
		^super.new(name, bounds, resizable, border, server, scroll).init
	}

	init {
		var width = this.bounds.width;
		var height = this.bounds.height;

		bigramSubdivisionSpace = bigramSpace / 6;  //(2 bigramSpace in a octave)

		mode = \insert;

		bigram = Bigram.new;

		// instanciate sliders

		vSlider = Slider(this,Rect(width-elementSize,0,elementSize,height-elementSize));
		hSlider = Slider(this,Rect(0,height-elementSize,width-elementSize,elementSize));
		zSlider = Slider2D(this,Rect(width-elementSize,height-elementSize,elementSize,elementSize));

		// instanciate buttons

		insertButton = Button(this, Rect(0, 0, elementSize, elementSize));
		editButton = Button(this, Rect(elementSize, 0, elementSize, elementSize));
		insertButton.valueAction_(1); //TODO: this is not working!!

		buttonList=List.new;
		buttonList.add(insertButton);
		buttonList.add(editButton);

		insertButton.states = [["I", Color.grey, Color.white], ["I", Color.white, Color.grey]];
		editButton.states = [["E", Color.grey, Color.white], ["E", Color.white, Color.grey]];

		insertButton.action_({|b| buttonList.do{|e|e.value=0}; b.value=1; mode=\insert; bigram.deselectAllNotes; this.refresh });
		editButton.action_({| b|buttonList.do{|e|e.value=0}; b.value=1; mode=\edit; this.refresh });

		// instanciate subdivision menu

		beatSubdivisionMenu = PopUpMenu(this,Rect(2*elementSize,0,2*elementSize,elementSize));
		beatSubdivisionMenu.items = ["1","2","3","4","5","6","7","8"];
		beatSubdivisionMenu.action = { |menu|
			beatSubdivision = menu.items[menu.value].asInt;
			this.refresh;
		};


		// instanciate view
		bigramView = UserView(this,Rect(0,elementSize,width-elementSize,height-(2*elementSize))).background_(Color.white);

		bigramView.drawFunc = {
			this.drawBigram;
			this.drawLines;
			this.drawCircles;
			this.drawSelectionRectangle;
		};

		// define actions

		bigramView.mouseDownAction = this.mouseDownAction;
		bigramView.mouseMoveAction = this.mouseMoveAction;
		bigramView.mouseUpAction = this.mouseUpAction;

		bigramView.keyDownAction = this.keyDownAction;



		// resize
		this.view.onResize_({
			this.resize;
			// this.refresh; --> automatic
		})
	}

	setBeatSubdivision { |newBeatSubdivision|
		beatSubdivision = newBeatSubdivision;
		this.refresh;
	}


	drawBigram {
		var width = this.bounds.width;
		var height = this.bounds.height;

		Pen.strokeColor = Color.black;
		(2*numOctaves + 1).do{ |i| // two octaves
			if (i.even) {
				Pen.width = 1.5;
			} {
				Pen.width = 0.75;
			};
			Pen.line(0@(i*bigramSpace+upperSpace),width@(i*bigramSpace+upperSpace));
			Pen.stroke;
		};

		// bigram subdivisions
		Pen.strokeColor = Color.red;
		Pen.width = 0.2;
		(12 * numOctaves).do {|i| //12 subdivisions per octave
			Pen.line(0@(i*bigramSubdivisionSpace+upperSpace),width@(i*bigramSubdivisionSpace+upperSpace))
		};
		Pen.stroke;
	}


	drawLines {
		var width = this.bounds.width;
		var height = this.bounds.height;

		var numBeatsToDraw;
		var numBeatSubdivisionsToDraw, beatSubdivisionLineSpace;


		//draw bars;         TODO: make it custom for each beat/bar
		numBeatsToDraw = (width / beatLineSpace).floor;
		Pen.strokeColor = Color.black;
		Pen.width = 0.4;
		numBeatsToDraw.do{ |i|
			Pen.line((i*beatLineSpace+leftSpace)@0,(i*beatLineSpace+leftSpace)@(height-elementSize));
		};
		Pen.stroke;

		//draw bar subdivisions
		Pen.width = 0.2;
		numBeatSubdivisionsToDraw = numBeatsToDraw * beatSubdivision;
		beatSubdivisionLineSpace = beatLineSpace / beatSubdivision;
		numBeatSubdivisionsToDraw.do{ |i|
			Pen.line((i*beatSubdivisionLineSpace+leftSpace)@0,(i*beatSubdivisionLineSpace+leftSpace)@(height-elementSize));
		};
		Pen.stroke;
	}



	resize {
		var width = this.bounds.width;
		var height = this.bounds.height;

		// sliders
		vSlider.moveTo(width-elementSize,0).resizeTo(elementSize,height-elementSize);
		hSlider.moveTo(0,height-elementSize).resizeTo(width-elementSize,elementSize);
		zSlider.moveTo(width-elementSize,height-elementSize).resizeTo(elementSize,elementSize);

		// view
		bigramView.resizeTo(width-elementSize,height-(elementSize*2))
	}

	mouseDownAction {^{ |view, x, y, modifiers, buttonNumber, clickCount|
		var xPos, yPos;
		lastMouseDownPosition = Point(x,y);

		// [x,y] are the mouse down coordinates, in pixels
		// [xPos,yPos] are the bigram coordinates, in the form (absolutePos,totalPitch)
		#xPos,yPos = this.getBigramCoordinate(x,y);

/*		"mousePosition: ".post;
		[x,y].postln;
		"bigramPosition: ".post;
		[xPos,yPos].postln;*/

		switch (mode)

		/////////////////////////// INSERT MODE /////////////////////
		{\insert} {
			//create new BigramNote
			var newNote, noteIndex;
			var pitchClass, height;
			var beat, relativePos;

			//calculate vertiscal position
			pitchClass = yPos % 12;
			height = (yPos / 12).floor;

			//calculate horizontal position

			beat = xPos.trunc;
			relativePos = xPos - beat;

			newNote = BigramNote.new(height,pitchClass,beat,relativePos);

			noteIndex = bigram.noteIndex(newNote);

			if (noteIndex.isNil) {
				//if there is no the same note in the same place, put the new note
				bigram.putNote(newNote)
			} {
				// if the is note, remove it
				bigram.removeNoteAt(noteIndex);
			};
		}

		/////////////////////////// EDIT MODE /////////////////////
		{\edit} {
			var newNote, noteIndex;
			var pitchClass, height;
			var beat, relativePos;

			//calculate vertical position
			pitchClass = yPos % 12;
			height = (yPos / 12).floor;

			//calculate horizontal position

			beat = xPos.trunc;
			relativePos = xPos - beat;

			// if there is a note in that position, select/deselect it
			newNote = BigramNote(height,pitchClass,beat,relativePos);
			noteIndex = bigram.noteIndex(newNote);
			if (noteIndex.isNil.not) {

				noteSelectedInLastDown = true;

				newNote = bigram.getNoteAt(noteIndex); // newNote now points to the existent note instance

				if (newNote.isSelected.not) {
					//if not selected, select
					newNote.isSelected_(true)
				}{
					//if selectec, deselect
					newNote.isSelected_(false)
				}
			} {

				// if there is no note in that position, create a selection rectangle!
				noteSelectedInLastDown = false;
			}
		}
		;
		this.refresh;
	}}

	mouseMoveAction { ^{ |view, x, y, modifiers|
		var selectedNotes, deselectedNotes;
		var xPos, yPos;

		var beat1, relativePos1, beat2, relativePos2;
		var height1, pitchClass1, height2, pitchClass2;


		if (mode == \edit and:{noteSelectedInLastDown.not}) {

			#xPos,yPos = this.getBigramCoordinate(x,y);
			selectionRectangle = Rect.fromPoints(lastMouseDownPosition,x@y);

			// convert to required separate format
			#beat1,relativePos1 = BigramNote.getRelativePos(this.getMouseXPosition(lastMouseDownPosition.x));
			#beat2,relativePos2 = BigramNote.getRelativePos(xPos);
			#height1,pitchClass1 = BigramNote.getRelativePitch(this.getMouseYPosition(lastMouseDownPosition.y));
			#height2,pitchClass2 = BigramNote.getRelativePitch(yPos);


			// select notes inside the rectangle
			selectedNotes = bigram.getNotesInRange(beat1, relativePos1, beat2, relativePos2, height1, pitchClass1, height2, pitchClass2);
			bigram.selectNotes(selectedNotes);

			// deselect notes which are now not selected
			deselectedNotes = List.new; // it could be an array, we know sizes of notes and selectedNotes
			bigram.notes.do{ |note|
				if (selectedNotes.indexOf(note).isNil) {
					deselectedNotes.add(note)
				}
			};
			bigram.deselectNotes(deselectedNotes);

			this.refresh;
		};
	}}

	mouseUpAction { ^{ |view, x, y, modifiers|
		if (mode == \edit) {
			selectionRectangle = nil;
		};
		this.refresh;
	}}

	keyDownAction { ^{ |view, char, modifiers, unicode, keycode, key|
		// key.postln;

		//global
		switch (key)
		{73} {insertButton.valueAction_(1)} // I: insert mode
		{69} {editButton.valueAction_(1)} //E: edit mode
		;

		//edit
		if (mode==\edit) {
			switch (key)

			// up arrow: move up 1 semitone selected notes
			{16777235} {bigram.moveSelectedNotes(\up,1)}

			// down arrow: move down 1 semitone selected notes
			{16777237} {bigram.moveSelectedNotes(\down,1)}

			// left arrow: move left 1 subdivision selected notes
			{16777234} {bigram.moveSelectedNotes(\left,1/beatSubdivision)}

			// right arrow: move right 1 subdivision selected notes
			{16777236} {bigram.moveSelectedNotes(\right,1/beatSubdivision)}

			// supr: remove selected notes
			{16777223} {bigram.removeSelectedNotes}


			;
		};
		this.refresh;
	}}

	getBigramCoordinate { |x,y|
		^[this.getMouseXPosition(x),this.getMouseYPosition(y)]
	}

	getMouseXPosition { |x|
		//calculates time subdivision from mouse position and offsets
		//numeration starts at 0
		var pos;
		if (x > leftSpace) {
			pos = x - leftSpace; // quit offset
			pos = pos / beatLineSpace;// <--------- TODO: adjust to be parametrizable!!
			//adjust to current beat subdivision
			pos = pos.round(1/beatSubdivision);
		} {
			//mouse in the leftspace
			pos = 0;
		}

		^pos;
	}

	getMouseYPosition { |y|
		//calculates pitch subdivision from mouse position and offsets
		//numeration starts at 0 = minimum pitch
		var pos;
		if (y > upperSpace) {
			pos = y - upperSpace;
			pos = (pos / bigramSubdivisionSpace).round;

		} { //TODO: just in case, don't cause an error
			pos = 0;
		};
		//turn numeration start
		if (pos.isNil.not) {
			pos = (numOctaves * 12) - pos;
			//TODO
			if (pos < 0) {pos=0};
		}
		^pos;
	}

	drawCircles {
		// get all notes
		bigram.getAllNotes.do{ |note|

			var beat = note.beat;
			var relativePos = note.relativePos;
			var height = note.height;
			var pitchClass = note.pitchClass;

			var xPos, yPos;
			var subdivisionTotal = (beat + relativePos) * beatSubdivision;
			var pitchTotal = (height*12) + pitchClass;
			//invert pitchTotal for the view
			pitchTotal = (numOctaves * 12) - pitchTotal;


			xPos = leftSpace + (subdivisionTotal*beatLineSpace/beatSubdivision);
			yPos = upperSpace + (pitchTotal*bigramSubdivisionSpace);

			//draw notes
			if (note.type == \w) {
				Pen.fillColor = Color.white;
			} {
				Pen.fillColor = Color.black;
			};

			if (mode == \edit){
				if (note.isSelected) {
					Pen.strokeColor = Color.blue;
					Pen.width = 2;
				} {
					Pen.strokeColor = Color.black;
					Pen.width = 1;
				};
			} {
				Pen.strokeColor = Color.black;
				Pen.width = 1;
			};

			Pen.addArc(xPos@yPos,beatLineSpace/2/beatSubdivision,0,2pi);
			Pen.fillStroke;
		}
	}

	drawSelectionRectangle {
		Pen.color = Color.blue;
		Pen.alpha = 0.2;
		Pen.addRect(selectionRectangle);
		Pen.fill;
	}


}

