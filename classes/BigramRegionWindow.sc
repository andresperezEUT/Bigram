////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) Andrés Pérez López, November 2014 [contact@andresperezlopez.com]
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; withot even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <<http://www.gnu.org/licenses/>
//
////////////////////////////////////////////////////////////////////////////

BigramRegionWindow  {
	//
	// 	var <bigramView;

	var <window;
	var insertButton, editButton;
	var buttonList;
	var playButton;
	var showBigramSubdivisionButton, showBigramSubdivision;

	var zoomPlusHButton, zoomMinusHButton;
	var zoomPlusVButton, zoomMinusVButton;

	var zoomSlider;

	// var beatSubdivisionMenu;
	//
	// 	var saveButton;
	// 	var loadButton;
	//
	var <margin = 50;
	var <elementSize = 30;
	var <bigramView;
	var <bigramView_height;
	var <bigramView_width;

	var <pulseWidth = 100;
	var <bigramHeight = 120; // one octave
	var <numOctaves;

	// 	var <bigramSpace = 30; //vertical
	// 	var <bigramSubdivisionSpace; //= bigramSpace / 6;  //(2 bigramSpace in a octave)
	// 	// margins of editView
	// 	var <upperSpace = 50;
	// 	var <lowerSpace = 50;
	// 	var <leftSpace = 50;
	// 	var <rightSpace = 50;
	//
	// 	var <beatLineSpace = 40; //horizontal (time subdivision)
	// 	var <beatSubdivision = 4;
	//
	// 	var <numBeats;
	// 	var <numOctaves = 8;
	//
	var lastMouseDownPosition;
	var selectionRectangle;
	var noteSelectedInLastDown;
	//
	// var <circleRadius = 20;
	//
	// 	var <mode;
	//
	//
	// internal representation
	//
	// references to the context
	var <editor;
	var <track;
	var <region;
	var <regionName;

	// parameters
	var <startBPD, <startPulseIndex;
	var <endBPD, <endPulseIndex;
	var <numPulses;
	var <>mode;

	var <divisionsFromPulses;
	var <barsFromPulses;
	var <pulsesFromPulseIndex;


	var <holdSelection = false;

	var optionsView;
	var optionsView_height;
	var canvasView;
	var canvasLeftMargin; // harcoded somewhere to pulseWidth/2

	var duplicateButton;

	var <>windowIndex;
	var <>trackName;

	var <>drawLines = false;

	//gui
	// var <showBigramSubdivision;
	//
	// 	// edit view limits
	// 	var <maxRightPosition, <maxDownPosition;
	//
	//
	*new { |editorWindow,track,trackName,region,regionName,windowIndex|
		^super.new.init(editorWindow,track,trackName,region,regionName,windowIndex);
	}

	reload {
		track = editor.bigramEditor.bigramTracks.at(editor.bigramEditor.bigramTrackNames.indexOf(trackName));
		region = track.bigramRegions.at(track.bigramRegionNames.indexOf(regionName));
		window.refresh;
	}

	init  { |myEditorWindow, myTrack, myTrackName, myRegion, myRegionName,myWindowIndex|

		windowIndex = myWindowIndex;
		window = Window.new("Bigram Region Editor " ++ windowIndex.asString);

		// configure
		window.front;
		window.view.onResize = {
			this.recalculateViewSize;
		};

		window.onClose = {
			editor.updateView;
			editor.bigramRegionWindowList.remove(this);
		};
		// window.view.hasBorder_(false);

		//get reference to the region and the bigram editor window
		editor = myEditorWindow;
		track = myTrack;
		trackName = myTrackName;
		region = myRegion;
		regionName = myRegionName;


		numOctaves = editor.bigramEditor.numOctaves;

		startBPD = region.startBPD;
		endBPD = region.endBPD;

		numPulses = region.numPulses;

		showBigramSubdivision = false;


		showBigramSubdivision = false;

		mode = \insert;

		////// options bar
		optionsView_height = elementSize; /*window.bounds.height / 8;*/
		optionsView = CompositeView(window,Rect(0,0,window.bounds.width,optionsView_height));
		optionsView.background_(Color.grey(0.2));

		insertButton = Button(optionsView, Rect(0, 0, elementSize, elementSize));
		editButton = Button(optionsView, Rect(elementSize, 0, elementSize, elementSize));

		buttonList=List.new; // only insert and edit
		buttonList.add(insertButton);
		buttonList.add(editButton);

		insertButton.states = [["I", Color.grey, Color.white], ["I", Color.white, Color.grey]];
		editButton.states = [["E", Color.grey, Color.white], ["E", Color.white, Color.grey]];

		insertButton.action_({|b| buttonList.do{|e|e.value=0}; b.value=1; mode=\insert; region.deselectAllNotes; this.refresh });
		editButton.action_({| b|buttonList.do{|e|e.value=0}; b.value=1; mode=\edit; this.refresh });

		insertButton.value_(1); //TODO: this is not working!!
		editButton.value_(0);

		/*		duplicateButton = Button(optionsView, Rect(2*elementSize, 0, elementSize, elementSize));
		duplicateButton.states = [["d", Color.blue, Color.grey(0.9)]];
		duplicateButton.action_({ region.duplicateSelectedNotes	});*/


		//// play button -> outside of buttonList (not a mode button)
		playButton = Button(optionsView, Rect(4*elementSize, 0, elementSize, elementSize));
		playButton.states = [["P", Color.blue, Color.grey(0.9)]/*,["P", Color.white, Color.blue(0.75)]*/];

		playButton.action_({ |b|
			var pbind = region.createPatterns;
			pbind.play;
			/*			if (region.eventStreamPlayer.isPlaying.not) {
			region.play;
			};*/
		});

		// showBigramSubdivisionButton

		showBigramSubdivisionButton = Button (optionsView, Rect(5*elementSize, 0, elementSize, elementSize));
		showBigramSubdivisionButton.states = [["s", Color.grey, Color.white], ["s", Color.white, Color.grey]];
		showBigramSubdivisionButton.action_({ |b|
			if (b.value == 0) {
				showBigramSubdivision = false;
			} {
				showBigramSubdivision = true;
			};
			this.refresh;
		});


		zoomSlider = Slider2D.new(optionsView, Rect(7*elementSize,0,elementSize*4,elementSize)).x_(0.5).y_(0.5);
		zoomSlider.action_({ |slider|

			pulseWidth = slider.x.linlin(0,1,10,90);
			bigramHeight = slider.y.linlin(0,1,200,50);
			//recalculate bigramView size
			this.resizeCanvas;

		});


		///// canvas view: parent of bigramView
		canvasView = ScrollView(window,Rect(0,optionsView_height,window.bounds.width, window.bounds.height - optionsView_height));
		canvasView.autohidesScrollers_(false);
		canvasView.hasBorder_(false);
		canvasView.background_(Color.grey(0.4));



		// bigram view limits
		bigramView_height = bigramHeight * numOctaves;
		bigramView_width = pulseWidth * (numPulses + 0.5);


		// instanciate view
		bigramView = UserView(canvasView,Rect(margin, elementSize.max(margin), bigramView_width, bigramView_height)).background_(Color.white);

		bigramView.clearOnRefresh_(false);

		// bigramView = UserView(this,Rect(margin, elementSize.max(margin), bigramView_width, bigramView_height)).background_(Color.white);



		///////////////////////////////////////////////////7


		bigramView.drawFunc = {
			["drawLines",drawLines].postln;
			this.drawBigram;
			["drawLines",drawLines].postln;
			"bigramViewDrawFunc".postln;
			this.drawNotes;
			this.drawSelectionRectangle;
			["drawLines",drawLines].postln;
			// editor.updateView; // update note changes in the bigram editor window
		};


		// define actions
		//
		bigramView.mouseDownAction = this.mouseDownAction;
		bigramView.mouseMoveAction = this.mouseMoveAction;
		bigramView.mouseUpAction = this.mouseUpAction;
		//
		bigramView.keyDownAction = this.keyDownAction;
		bigramView.keyUpAction = this.keyUpAction;


	}

	resizeCanvas {
		bigramView_height = bigramHeight * numOctaves;
		bigramView_width = pulseWidth * (numPulses + 0.5); // half pulse more for view
		bigramView.bounds_(Rect(margin, elementSize.max(margin), bigramView_width, bigramView_height));
		drawLines = true;
		this.refresh;

	}

	drawBigram {
		var width = window.bounds.width;
		var height = window.bounds.height;

		var numBeatSubdivisionsToDraw, beatSubdivisionLineSpace;
		var subdivisionHeight;


		if (drawLines) {

			"----DRAW LINES----".postln;



			/////////// HORIZONTAL LINES ////////////
			//
			// bigram lines
			//

			Pen.strokeColor = Color.black;
			(2*numOctaves + 1).do{ |i| // two octaves
				if (i.even) {
					Pen.width = 1.5;
				} {
					Pen.width = 0.75;
				};

				Pen.line(0@(i*bigramHeight/2), bigramView_width@(i*bigramHeight/2));
				// Pen.line(leftSpace@(i*bigramSpace+upperSpace),maxRightPosition@(i*bigramSpace+upperSpace));
				Pen.stroke;
			};

			//
			// bigram subdivisions
			//

			subdivisionHeight = bigramHeight / 6;
			if (showBigramSubdivision) {

				Pen.strokeColor = Color.red;
				Pen.width = 0.2;
				(12 * numOctaves).do {|i| //12 subdivisions per octave

					Pen.line(0@(i*subdivisionHeight), bigramView_width@(i*subdivisionHeight));

				};
				Pen.stroke;
			};
			//
			// //
			// //
			// //
			///////// VERTICAL LINES ////////////

			// avoid computation due to left margin
			Pen.translate(pulseWidth*0.5,0);

			// pulse lines

			Pen.strokeColor = Color.black;
			Pen.width = 0.4;
			(numPulses+1).do{ |i|
				Pen.line((i*pulseWidth)@0,(i*pulseWidth)@bigramView_height);
			};
			Pen.stroke;


			// pulse subdivision lines

			Pen.width = 0.2;
			region.divisionsFromPulses.do{ |divisions,pulse|
				divisions.do { |d|
					Pen.line(
						Point((pulse*pulseWidth)+((d)*pulseWidth/divisions),0),
						Point((pulse*pulseWidth)+((d)*pulseWidth/divisions),bigramView_height)
					);
				}
			};
			Pen.stroke;

			// bar lines

			Pen.width = 1;
			region.barsFromPulses.do { |bar,pulse|
				if (bar != region.barsFromPulses[(pulse-1).wrap(0,region.barsFromPulses.size-1)]) {
					Pen.line(
						Point(pulse*pulseWidth,0),
						Point(pulse*pulseWidth,bigramView_height)
					);
				}
			};
			Pen.stroke;

			Pen.translate(pulseWidth*0.5.neg,0);

			// grey left margin
			// /2 because hardcoded above (+ 0.5) in resizeCanvas
			Pen.addRect(Rect(0,0,pulseWidth/2,bigramView_height));
			Pen.fillColor = Color.grey(0.8,alpha:0.5);
			Pen.fill;

			// piano roll
			Pen.fillColor = Color.black;
			Pen.strokeColor = Color.black;
			Pen.addRect(Rect.fromPoints( Point(0,0), Point(pulseWidth/4,bigramView_height) ));
			Pen.fill;

			Pen.translate(0,subdivisionHeight/4);
			Pen.fillColor = Color.white;
			(bigramView_height / subdivisionHeight).do { |i|
				var height = i * subdivisionHeight;
				Pen.addRect(Rect(0,height,pulseWidth/4,subdivisionHeight/2));
				Pen.fillStroke;
			};
			Pen.translate(0,subdivisionHeight/2.neg);
			// octave numbers
			numOctaves.do { |i|
				if (i > 0) {
					var y  = i * bigramHeight;
					var string = (i - numOctaves).abs.asString;
					Pen.stringAtPoint(string,Point(0,y),Font("Helvetica-Bold", 20),Color.red);
				}
			};
			Pen.translate(0,subdivisionHeight/4);
		};
		drawLines = false;

	}

	// get a bigramCoordinate [bpd,absPitch]
	getBigramNote { |x,y|
		// var aa ="region bpdList";
		// var a = region.bpdList.do(_.print);

		var pulseDivisionWidth = pulseWidth / 2;
		var bigramDivisionHeight = bigramHeight / 12;

		var bpd, pitch;
		var divisionIndex;
		// var divisionIndex = (x / pulseDivisionWidth).round;
		var yPos = ((numOctaves * 12) - (y / bigramDivisionHeight)).round;

		if (x / pulseWidth < 0.25) { // piano roll
			// if ((x / pulseWidth / 2) < 0 ) { // avoid left margin
			"left".postln;
			divisionIndex = 0;
		} {
			// var a = "pulseIndex".postln;
			var pulseIndex = ((x/pulseWidth) - 0.5).abs.trunc;
			var q = ["pulseIndex",pulseIndex].postln;

			// var q = "divisions".postln;
			// region.divisionsFromPulses.postln;
			var div = region.divisionsFromPulses.at(pulseIndex);
			var s = ["div",div].postln;

			var frac = (((x/pulseWidth) - 0.5).abs - pulseIndex);
			var a = ["frac",frac].postln;
			var divInPulse = (frac / (1/div)).round/*floor*/;
			var d = ["divInPulse",divInPulse].postln;

			// get pulse index
			divisionIndex = 0;
			(pulseIndex).do { |i| divisionIndex = divisionIndex + region.divisionsFromPulses.at(i)};
			// region.divisionsFromPulses(0..pulseIndex-1).do{|div| divisionIndex = divisionIndex + div};
			divisionIndex = divisionIndex + divInPulse;
			// "res".postln;
			// divisionIndex.postln;

			// ((x / (pulseWidth/div)) - 1).postln.round.postln;

			/*var divisions = region.bpdList.at(pulseIndex).division.postln;*/

			// var pulseIndex = ((x / pulseWidth)).postln.round.postln;
			// divisionIndex = ((x / pulseDivisionWidth) - 1).round;
		};

		// turn xPos into bpd
		bpd = region.bpdList.at(divisionIndex.clip(0,region.bpdList.size-1));
		pitch = BPitch.new((yPos/12).floor,yPos%12);
		// "+++".postln;

		^BigramNote.new(pitch.print,bpd.print);
	}


	mouseDownAction {^{ |view, x, y, modifiers, buttonNumber, clickCount|
		var note, noteIndex;
		lastMouseDownPosition = Point(x,y);

		"MOUSE_DOWN".postln;

		/*		"pointer".postln;
		[x,y].postln;*/

		if ( x < (pulseWidth/2) ) {
			// click on piano: only listen
			note = this.getBigramNote(x,y);
			this.testNote(note);

		} {
			// [x,y] are the mouse down coordinates, in pixels
			// [xPos,yPos] are the bigram coordinates, in the form (absolutePos,totalPitch)
			note = this.getBigramNote(x,y);
			noteIndex = region.hasNote(note);

			/*		"mousePosition: ".post;
			[x,y].postln;
			"bigramPosition: ".post;
			[xPos,yPos].postln;*/

			switch (mode)
			{\insert} {
				region.deselectAllNotes;
				// if there is no note, place one
				if (noteIndex.isNil) {
					note.isSelected_(true);
					region.putNote(note);
					"PLACE NEW NOTE".postln;
					this.drawLines_(false);
					["drawLines",drawLines].postln;
					bigramView.refresh; ///////////////////////////////////// <------------------------
					// send test note
					this.testNote(note);
				} {
					// if there is note, remove it
					"REMOVE NOTE".postln;
					region.removeNoteAt(noteIndex);
					bigramView.clearDrawing;
					drawLines = true;
					bigramView.refresh;
				};

			}
			{\edit} {
				// region.deselectAllNotes;
				if (noteIndex.isNil.not) {
					noteSelectedInLastDown = noteIndex;
					// if there is note, [de]select it
					// region.getNoteAt(noteIndex).isSelected_(region.getNoteAt(noteIndex).isSelected.not);
				} {
					// if not, start selection area
					noteSelectedInLastDown = nil;
				};
				bigramView.clearDrawing;
				drawLines = true;
				bigramView.refresh;
			};

		};

	}}

	mouseMoveAction { ^{ |view, x, y, modifiers|

		// region.deselectAllNotes;

		"MOUSE_MOVE".postln;

		if (mode == \edit) {
			if (noteSelectedInLastDown.isNil.not) {
				// noteIndex
				var newNote = this.getBigramNote(x,y);
				var lastSelectedNote = region.getNoteAt(noteSelectedInLastDown);
				var diffPitch = newNote.pitch.absPitch - lastSelectedNote.pitch.absPitch;
				var diffDiv;
				var newNoteIndex, lastNoteIndex;
				var update = false;
				region.bpdList.do{ |bpd,i|
					if (bpd.equal(newNote.bpd)) {newNoteIndex = i};
					if (bpd.equal(lastSelectedNote.bpd)) {lastNoteIndex = i};
				};

				diffDiv = newNoteIndex - lastNoteIndex;
				// newNote.print;
				/*				lastSelectedNote.print;*/
				// [diffPitch,diffDiv].postln;


				if (diffPitch > 0) {
					region.moveSelectedNotes(\up,diffPitch);
					update=true;
				};
				if (diffPitch < 0) {
					region.moveSelectedNotes(\down,diffPitch.abs);
					update=true;
				};

				if (diffDiv > 0) {
					region.moveSelectedNotes(\right,diffDiv.abs);
					update=true;
				};
				if (diffDiv < 0) {
					region.moveSelectedNotes(\left,diffDiv.abs);
					update=true;
				};


				/*region.getSelectedNotes.do{ |n|

				n.pitch_(newNote.pitch);
				n.bpd_(newNote.bpd)
				};*/
				if (update) {
					drawLines = true;
					bigramView.clearDrawing;
					this.refresh;
				}

			} {
				//draw rectangle
				var note = this.getBigramNote(x,y);
				var lastNote = this.getBigramNote(lastMouseDownPosition.x,lastMouseDownPosition.y);

				if (holdSelection.not) {
					region.deselectAllNotes;
				};

				region.getNotesBetween(note,lastNote).do{|n|
					n.isSelected_(true);
				};

				// update selection rectangle
				selectionRectangle = Rect.fromPoints(lastMouseDownPosition,x@y);
				drawLines = true;
				bigramView.clearDrawing;
				this.refresh;
			}

		}

	}}

	mouseUpAction { ^{ |view, x, y, modifiers|

		"MOUSE_UP".postln;

		if (mode == \edit) {
			selectionRectangle = nil;
			// this.drawSelectionRectangle;
			drawLines = true;
			bigramView.clearDrawing;
			this.refresh;
		};

	}}


	keyDownAction { ^{ |view, char, modifiers, unicode, keycode, key|
		key.postln;
		//
		//global
		switch (key)
		{73} {insertButton.valueAction_(1)} // I: insert mode
		{69} {editButton.valueAction_(1)} // E: edit mode

		// up arrow: move up 1 semitone selected notes
		{16777235} {region.moveSelectedNotes(\up,1);}

		// down arrow: move down 1 semitone selected notes
		{16777237} {region.moveSelectedNotes(\down,1);}

		// left arrow: move left 1 subdivision selected notes
		{16777234} {region.moveSelectedNotes(\left,1)}

		// right arrow: move right 1 subdivision selected notes
		{16777236} {region.moveSelectedNotes(\right,1)}

		// supr: remove selected notes
		{16777223} {region.removeSelectedNotes}

		// control: hold selection
		{16777249} {holdSelection=true}

		// space: play
		{32} {region.createPatterns.play}

		// d: duplicate selected notes
		{68} {region.duplicateSelectedNotes}

		{90} {
			if (modifiers == 262144) {
				if (editor.bigramEditor.tmpVersion >= 1 ) {
					var path;

					// remove tracks
					editor.removeAllTracks;
					// remove bars
					editor.bigramEditor.tempos= List.new;
					editor.bigramEditor.barsFromPulses = List.new;
					editor.bigramEditor.numBars = 0;
					editor.bigramEditor.measuresFromBars = List.new;
					// remove bar views
					editor.recalculateCanvasSize;

					editor.bigramEditor.tmpVersion = editor.bigramEditor.tmpVersion - 1;
					path = editor.bigramEditor.tmpFilePath ++ "_" ++ editor.bigramEditor.tmpVersion.asString;
					editor.loadTmp(path);
					["bigramEditor.tmpVersion",editor.bigramEditor.tmpVersion].postln;
					["bigramEditor.tmpMaxVersion",editor.bigramEditor.tmpMaxVersion].postln;

				} {
					"original state".postln;
				}
			};
			if (modifiers == 393216) { //redo
				var path;

				//load
				if (editor.bigramEditor.tmpVersion < editor.bigramEditor.tmpMaxVersion) {

					// remove tracks
					editor.removeAllTracks;
					// remove bars
					editor.bigramEditor.tempos= List.new;
					editor.bigramEditor.barsFromPulses = List.new;
					editor.bigramEditor.numBars = 0;
					editor.bigramEditor.measuresFromBars = List.new;
					// remove bar views
					editor.recalculateCanvasSize;

					editor.bigramEditor.tmpVersion = editor.bigramEditor.tmpVersion + 1;
					path = editor.bigramEditor.tmpFilePath ++ "_" ++ editor.bigramEditor.tmpVersion.asString;
					editor.loadTmp(path);
					["bigramEditor.tmpVersion",editor.bigramEditor.tmpVersion].postln;
					["bigramEditor.tmpMaxVersion",editor.bigramEditor.tmpMaxVersion].postln;
				} {
					"last version".postln;
				};
			}
		}
		;
		// };
		drawLines = true;
		bigramView.clearDrawing;
		this.refresh;
	}}

	keyUpAction { ^{ |view, char, modifiers, unicode, keycode, key|
		// if (mode==\edit) {
		switch (key)
		// control: hold selection
		{16777249} {holdSelection=false}
		;
		// }
	}}


	////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////

	getBigramCoordinate { |note|

		// var a = "getBigramCoordinate".postln;
		// var r = "subdivisionIndex".postln;
		var subdivisionIndex = region.getSubdivisionIndex(note);
		// var subdivisionIndex = region.bpdList.indexOf(note.bpd);
		var absPitch = (numOctaves * 12) - note.pitch.absPitch;

		var y = absPitch * bigramHeight / 12;
		var x = 0;



		// var pulseCount = region.divisionsFromPulses[0];
		// if (subdivisionIndex < pulseCount) {
		// 	// subdivision está aqui. calcular x
		// } {
		//
		// };
		// region.divisionsFromPulses.do { |i|
		// 	if (subdivisionIndex
		// 	}

		var finished = false;
		var acu = 0;
		var acuDiv = List.new;
		var divInPulse = subdivisionIndex;
		region.divisionsFromPulses.do { |div|
			acuDiv.add(acu);
			acu = acu + div;
		};
		// "acuDiv".postln;
		// acuDiv.postln;

		(acuDiv.size-1).do { |i|
			// ["i",i,"subdivisionIndex",subdivisionIndex,"acuDiv[i]",acuDiv[i],"acuDiv[i+1]",acuDiv[i+1]].postln;
			if (subdivisionIndex >= acuDiv[i] and:{subdivisionIndex < acuDiv[i+1]}) {
				// está en este slot: calcular posición
				var div = region.divisionsFromPulses[i];
				x = x + (pulseWidth/2) + (divInPulse*pulseWidth/div);

				// x = x + (pulseWidth/div) + (divInPulse*pulseWidth/div);
				// "aqui".postln;
				finished = true;
				// ["divInPulse",divInPulse].postln;
			} {
				if (finished.not) {
					var div = region.divisionsFromPulses[i];
					divInPulse = divInPulse - div;
					// ["divInPulse",divInPulse].postln;
					// "sumar 1 pulso".postln;
					// no está en el slot: sumar 1 pulso
					x = x + pulseWidth;
				}
			};
		};
		if (finished.not) {
			// var q = "NOT FINISHED".postln;
			var  i = acuDiv.size - 1;
			var div = region.divisionsFromPulses[i];
			x = x + (pulseWidth/2) + (divInPulse*pulseWidth/div);
			// ["divInPulse",divInPulse].postln;
		};
		// x.postln;
		// x = subdivisionIndex + 1 * pulseWidth / 2;
		// "get bigram coordinate".postln;
		^[x,y]
	}


	drawNotes {
		var circleRadius = min(pulseWidth/2,bigramHeight/12);
		// "DRAW NOTES".postln;
		var canvasOrigin = canvasView.visibleOrigin;
		var canvasWidth = canvasView.bounds.width;
		var canvasHeight = canvasView.bounds.height;

		region.getAllNotes.do{ |note|
			var x,y;

			// "bigramCoordinate".postln;
			#x,y = this.getBigramCoordinate(note);

			/*			"canvasView.canvas.bounds".postln;
			canvasView.canvas.bounds.postln;*/

			/*			if (x > canvasOrigin.x and:{x < (canvasOrigin.x + canvasWidth)}) {
			if (y > canvasOrigin.y and:{y < (canvasOrigin.y + canvasHeight)}) {*/

			if (note.getType == \w) {
				Pen.fillColor = Color.white;
			} {
				Pen.fillColor = Color.black;
			};

			// if (mode == \edit) {
			if (note.isSelected) {
				Pen.strokeColor = Color.blue;
				Pen.width = 2;
			} {
				Pen.strokeColor = Color.black;
				Pen.width = 1;
			};
			// } {
			// 	Pen.strokeColor = Color.black;
			// 	Pen.width = 1;
			// };

			Pen.addArc(x@y,circleRadius,0,2pi);
			Pen.fillStroke;
			// note.print;

			/*				}
			}*/

		}
	}


	drawSelectionRectangle {
		Pen.color = Color.blue;
		Pen.alpha = 0.2;
		Pen.addRect(selectionRectangle);
		Pen.fill;
	}
	//
	//
	//

	refresh {
		canvasView.refresh;
	}

	recalculateViewSize {
		optionsView.bounds_(Rect(0,0,window.bounds.width,optionsView_height));
		canvasView.bounds_(Rect(0,optionsView_height,window.bounds.width, window.bounds.height - optionsView_height));
		drawLines = true;
		this.refresh;

	}

	testNote { |note|
		Task{
			// -3 because note at the line is A
			editor.midiOut.noteOn(track.midiChannel,note.pitch.absPitch-3,127);
			0.5.wait;
			editor.midiOut.noteOff(track.midiChannel,note.pitch.absPitch-3,127);
		}.play;
	}
}
//
//
