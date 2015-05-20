+ BigramEditor {

	exportTrack2pdf { |trackName, pdfPath|

		var midiFile;
		var midiFilePath;

		var lyFile;
		var lyFilePath;
		var midi2lyCmd;

		var notesString;
		var currentLine;

		var noteList;

		var octaves;
		var reducedOctaves;

		var index;

		var defaultBigramStaffCoordinates;
		var bigramStaffCoordinates;
		var extraBigramStaffCoordinates;

		var formatFile1, formatString1;
		var formatFile2, formatString2;
		var staffSymbolString;

		var pdfFilePath;

		// select track

		var track = this.bigramTracks.at(this.bigramTrackNames.indexOf(trackName)); // !!!!!

		// convert to MIDI

		var pattern = track.createPatterns;
		midiFilePath = Platform.defaultTempDir ++ "bigram_midi.mid";
		midiFile = SimpleMIDIFile( midiFilePath );
		midiFile.init1( 2, 120, "4/4" ); /// !!!!!!!
		midiFile.timeMode = \seconds;
		midiFile.fromPattern( pattern );
		midiFile.write;

		// convert from MIDI to ly

		/*lyFilePath = "/home/pans/Escritorio/bigram_ly.ly";*/
		lyFilePath = Platform.defaultTempDir ++ "bigram_ly.ly";
		midi2lyCmd = "midi2ly -a -o " ++ lyFilePath ++ " " ++ midiFilePath;
		midi2lyCmd.systemCmd;

		// parse notes from ly

		lyFile = File.new(lyFilePath,"r");
		notesString = String.newClear;

		currentLine = lyFile.getLine;
		while ({currentLine[0..13] != "trackBchannelB"}) {
			currentLine = lyFile.getLine;
		};

		currentLine = lyFile.getLine;	// here are the notes
		while ({currentLine != "}"}) {
			notesString = notesString ++ currentLine;
			currentLine = lyFile.getLine;
		};

		lyFile.close;

		// remove comments

		index = notesString.indexOf($%);
		while ({index.isNil.not}) {
			notesString.removeAt(index);
			while ({notesString[index].ascii < 65}) {notesString.removeAt(index)};
			index=notesString.indexOf($%);
		};

		// get register from midi

		noteList = List.new;
		reducedOctaves = List.new;

		midiFile.noteOnEvents.do { |e|
			noteList.add(e[4])
		};
		octaves = ( (noteList + 3) / 12 ).floor; // reference from A
		reducedOctaves = octaves.as(Set).as(Array).sort; // remove duplicates
		reducedOctaves = (reducedOctaves.first .. reducedOctaves.last); // middle staffs
		/*		octaves.do { |o|
		if ( reducedOctaves.indexOf(o).isNil ) { reducedOctaves.add(o) };
		};*/

		defaultBigramStaffCoordinates = [-9, -3, 3]; // octave 5

		reducedOctaves.do{ |o|
			bigramStaffCoordinates = bigramStaffCoordinates ++ (defaultBigramStaffCoordinates + (12*(o-5)))
		};
		bigramStaffCoordinates = bigramStaffCoordinates.as(Set).as(Array).sort; // remove duplicates

		extraBigramStaffCoordinates = List.new;
		bigramStaffCoordinates.do { |c,i|
			if (i.even) {
				extraBigramStaffCoordinates.add(c - 0.1);
				extraBigramStaffCoordinates.add(c + 0.1);
			}
		};
		bigramStaffCoordinates = bigramStaffCoordinates ++ extraBigramStaffCoordinates;



		// create ly file with the notes and the bigram format

		formatFile1 = File.new(Platform.userExtensionDir ++ "/bigram/lilypond/format1.txt","r");
		formatString1 = String.readNew(formatFile1);
		formatFile1.close;

		formatFile2 =File.new(Platform.userExtensionDir ++ "/bigram/lilypond/format2.txt","r");
		formatString2 = String.readNew(formatFile2);
		formatFile2.close;

		lyFile = File.new(lyFilePath,"w"); // reuse the file variable
		formatString1.do{|s| lyFile.write(s)};

		lyFile.write("notes =  {");
		notesString.do{|s| lyFile.write(s)};
		lyFile.write("}");

		formatString2.do{|s| lyFile.write(s)};

		// add the staff coordinates
		staffSymbolString = " \\override StaffSymbol #'line-positions = #'( ";
		bigramStaffCoordinates.do{|c|
			staffSymbolString = staffSymbolString ++ c ++ " ";
		};
		staffSymbolString = staffSymbolString ++ ")\n}\n}\n}";
		lyFile.write(staffSymbolString);

		lyFile.close;

		// call lilypond
		pdfFilePath = pdfPath;
		("lilypond -o " ++ pdfFilePath ++ " -fpdf " ++ lyFilePath).systemCmd;
	}
}
